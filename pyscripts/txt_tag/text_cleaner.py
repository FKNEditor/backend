import string
import nltk
import sys
import json
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
from nltk.tag import pos_tag
from collections import defaultdict

nltk.download('stopwords', download_dir='usr/local/lib/nltk_data')
nltk.download('wordnet', download_dir='usr/local/lib/nltk_data')
nltk.download('averaged_perceptron_tagger', download_dir='usr/local/lib/nltk_data')

lemmatizer = WordNetLemmatizer()
stop_words = set(stopwords.words("english"))

def title_cleanup(titles, id_value):
    '''
    Returns a list, with words forming the title of the edition/stories, and prints to stdout word,edition_id,1

            Parameters:
                    titles (list[str]): list of strings consisting of each title
                    id_value (int): edition id

            Returns:
                    stop_rem (list[str]): list of unique words encountered
    '''

    title_words = ' '.join(titles).lower()
    title_words_set = set(title_words.split())
    stop_rem = sorted([word for word in title_words_set if word not in stop_words])
    for word in stop_rem:
        print(f"{word},{id_value},1", file=sys.stdout)
    return stop_rem

def author_cleanup(authors, id_value, prev_words):
    '''
    Prints to stdout word,edition_id,1

            Parameters:
                    authors (list[str]): list of strings consisting of each author
                    id_value (int): edition id
                    prev_words (list[str]): list of unique words previously encountered
    '''
    author_names = ' '.join(authors).lower()
    author_names_set = set(author_names.split())
    for name in author_names_set:
        if(name not in prev_words):
            print(f"{name},{id_value},1", file=sys.stdout)
        prev_words.append(name)
    prev_words = list(set(prev_words))

def proper_nouns_cleanup(texts, id_value, prev_words):
    '''
    Prints to stdout word,edition_id,1

            Parameters:
                    texts (list[str]): list of strings consisting of all texts
                    id_value (int): edition id
                    prev_words (list[str]): list of unique words previously encountered
    '''
    single_line = ' '.join(texts).replace("\\n", ' ').replace('\n', ' ')
    punc_rem = single_line.translate(single_line.maketrans("", "", string.punctuation))
    token = punc_rem.split()
    tagged_sent = pos_tag(token)
    proper_nouns = set([word for word,pos in tagged_sent if pos == 'NNP'])
    for proper_noun in proper_nouns:
        if proper_noun not in prev_words:
            print(f"{proper_noun},{id_value},1", file=sys.stdout)
        prev_words.append(proper_noun)
    prev_words = list(set(prev_words))

def text_cleanup(texts, id_value, prev_words):
    '''
    Prints to stdout word,edition_id,ratio

            Parameters:
                    texts (list[str]): list of strings consisting of all texts
                    id_value (int): edition id
                    prev_words (list[str]): list of unique words previously encountered
    '''
    single_line = ' '.join(texts).replace("\\n", ' ').replace('\n', ' ')
    punc_rem = single_line.translate(single_line.maketrans("", "", string.punctuation)) # Remove punctuation
    case_normal = punc_rem.lower().split()
    num_rem = [word for word in case_normal if not (any(char.isdigit() for char in word))] # Remove numbers
    lem = [lemmatizer.lemmatize(word) for word in num_rem] # Lemmatize words
    stop_rem = [word for word in lem if word not in stop_words] # Remove stop words
    clean_text = sorted(stop_rem)
    len_clean_text = len(clean_text)
    ratio = defaultdict(int)
    for word in clean_text:
        if word not in prev_words:
            ratio[word] += 1
    for key in ratio:
        ratio[key] /= len_clean_text
        print(f"{key},{id_value},{round(ratio[key], 4)}", file=sys.stdout)



# Read the JSON data from stdin
json_text = sys.stdin.read()
titles = []
authors = []
texts = []
try:
    # Parse the JSON data
    data = json.loads(json_text)
    
    # Access the fields
    id_value = data["id"]
    edition_title = data["title"]
    stories = data["stories"]
    titles.append(edition_title)

    for i, story in enumerate(stories, start=1):
        titles.append(story['title'])
        authors.append(story['author'])
        texts.append(story['text'])
    
    prev_words = title_cleanup(titles, id_value)
    author_cleanup(authors, id_value, prev_words)
    proper_nouns_cleanup(texts, id_value, prev_words)
    text_cleanup(texts, id_value, prev_words)


except json.JSONDecodeError as e:
    print("Error decoding JSON:", e)
