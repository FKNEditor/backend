import fitz
import argparse
import json
import sys
from column_boxes_rect import column_boxes_rect
from collections import defaultdict

# constants for tuples in the fitz (pymupdf) module
X0 = 0
Y0 = 1
X1 = 2
Y1 = 3

""" 

Step 1: Parse arguments 

"""
# parses "X0,Y0,X1,Y1" into (x0,y0,x1,y1) for use in argparse
def rect(s):
    try:
        x0, y0, x1, y1 = map(int, s.split(','))
        return x0, y0, x1, y1
    except:
        raise argparse.ArgumentTypeError("Coordinates must be x0,y0,x1,y1")

parser = argparse.ArgumentParser(prog='PDF Extractor',
                                 description='Extracts text in columns',
                                 epilog="""
                                        Brought to you by the PDF Super 
                                        Backend Alleyway Highway
                                        """)

def bbox(b):
    return fitz.IRect(b[:4])

ref_arg1 = parser.add_argument('-f', 
                               '--file-path', 
                               action='store', 
                               type=str)
ref_arg2 = parser.add_argument('-p', 
                               '--page-number', 
                               action='store', 
                               type=int)
ref_arg3 = parser.add_argument('-c',
                               '--clips', 
                               action='store', 
                               nargs='+', 
                               type=rect) # scans in a list (not a tuple)
ref_arg4 = parser.add_argument('-i', 
                               '--stdin', 
                               action='store_true')

args = parser.parse_args()
file_path = str(args.file_path) if args.file_path else None  
page_number = int(args.page_number) if args.page_number else 0
clips = [fitz.IRect(c) for c in args.clips] if args.clips else []
from_stdin = args.stdin 

""" 

Step 2: Attempt to load document and page

"""

# pdf document from PyMuPDF
doc = None

# A file needs to e specified
if (file_path==None and from_stdin==False):
    raise argparse.ArgumentError(ref_arg1, 
                                """
                                No file was specfied. Use -f to specify 
                                a file path, or if this was intended, use -i if 
                                you would like to read data through python's 
                                stdin
                                """)

# cant have both a file path and stdin data
if (file_path!=None and from_stdin==True): 
    raise argparse.ArgumentError(ref_arg1, 
                                """
                                Both -f/--file-path and -i/--stdin were
                                specified. Only one of these arguments should be
                                specified.
                                """)

# load document, prioritise file
if (file_path!=None):
    doc = fitz.open(str(file_path))
elif (from_stdin==True):
    data = sys.stdin.buffer.read()
    doc = fitz.open(stream=data, filetype='pdf')

# load page from document
if (page_number not in range(len(doc))):
    raise argparse.ArgumentError(ref_arg2, 
                                """
                                Page number specified does not exist! (We're 
                                using 0 based indexes because we're programmers 
                                :D)
                                """)
page = doc.load_page(page_number)

"""

Step 3: Extract text and image data

"""

# setting up json fields
extract_dict = defaultdict()
extract_dict['paragraphs'] = []
extract_dict['other'] = []
extract_dict['images'] = []

# if no clip was specfied, scan the whole page
if (clips==[]):
    page_clip = +page.rect
    page_clip.y0 += 50 # ignore top header
    page_clip.y1 -= 50 # ignore bottom header
    clips.append(page_clip)

# preload text and images as an optimisation
text_page = page.get_textpage()
text_blocks = text_page.extractBLOCKS()
text_block_set = set()
images = page.get_image_info(xrefs=True)
image_xref_bbox = [(i['xref'], fitz.IRect(i['bbox'])) for i in images]
image_xref_set = set()

# get text from rectangles
for clip in clips:

    # if there are no text blocks to extract, output the json file
    if (len(text_blocks) == 0):
        json.dump(dict(extract_dict), fp=sys.stdout, ensure_ascii=False)
        quit()

    # generate columns in the given rect
    bboxes = column_boxes_rect(page, clip=clip, no_image_text=True)

    if (len(bboxes) < 1):
        continue

    # get median width of boxes
    bbox_widths = [b.width for b in bboxes]
    median_width = bbox_widths[len(bbox_widths)//2]

    # filters out wider or thinner text boxes, assume columns are ordered left to right
    bboxes_main = [                         \
            b for b in bboxes               \
            if (b.width/median_width) < 1.5 \
            or  b.height/b.width > 0.6      \
            ]
    bboxes_main = sorted(bboxes_main, key=lambda b:b.x0)
    bboxes_other = [b for b in bboxes if b not in bboxes_main]

    # extract text from outlier bboxes
    for outlier_bbox in bboxes_other:
        outlier_tbs = [                             \
                tb for tb in text_blocks            \
                if bbox(tb).intersects(outlier_bbox)\
                ]
        outlier_tbs = [tb for tb in outlier_tbs if tb not in text_block_set] 
        outlier_tbs = sorted(outlier_tbs, key=lambda b:b[Y0])
        for tb in outlier_tbs:
            text_block_set.add(tb)
            text = ' '.join(tb[4].split())
            extract_dict['other'].append(text)

    # go through each column and extract the text blocks
    for column_bbox in bboxes_main:
        column_tbs = [                              \
                tb for tb in text_blocks            \
                if bbox(tb).intersects(column_bbox) \
                ]
        column_tbs = [tb for tb in column_tbs if tb not in text_block_set] 
        column_tbs = sorted(column_tbs, key=lambda b:b[Y0])

        # extract data from all paragraphs in the column
        while(not column_tbs == []):
            
            # next text block to extract in the column
            head, *tail = column_tbs 

            # filter column_tbs into two lists
            intersect = [head]
            non_intersect = []

            # find all other text blocks that intersects the current block
            for tb in tail:
                if bbox(head).intersects(bbox(tb)):
                    intersect.append(tb)
                else:
                    non_intersect.append(tb)
            
            # update text blocks that have been parsed
            text_block_set.update(intersect)

            # assume intersecting text blocks form one paragraph
            paragraph_text = "".join(
                    [                       \
                    " ".join(tb[4].split()) \
                    for tb in intersect     \
                    ])
            extract_dict['paragraphs'].append(paragraph_text)
 
            column_tbs = non_intersect

    # extract images
    for xref, im_bbox in image_xref_bbox:

        if xref not in image_xref_set and im_bbox.intersects(clip):
            img_text = [                    \
                    ' '.join(tb[4].split()) \
                    for tb in text_blocks   \
                    if bbox(tb) in im_bbox  \
                    ]
            img_obj = {"xref": xref, "text": img_text}
            extract_dict['images'].append(img_obj)
            image_xref_set.add(xref)
            text_block_set.update(img_text)
    
    # filter text blocks to get the remaining text blocks
    text_blocks = list(filter(lambda tb: tb not in text_block_set, text_blocks))

# parse results into a json file
json.dump(dict(extract_dict), fp=sys.stdout, ensure_ascii=False)
