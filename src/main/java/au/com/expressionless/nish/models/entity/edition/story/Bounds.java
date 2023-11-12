package au.com.expressionless.nish.models.entity.edition.story;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;

@Embeddable
public class Bounds {
    
    @NotNull
    private Integer x;

    @NotNull
    private Integer y;
    
    @NotNull
    private Integer width;

    @NotNull
    private Integer height;

    @NotNull 
    private Integer sequenceNum;

    public Bounds() {
        /* no-args constructor */
    }

    public Bounds(int x, int y, int width, int height, int sequenceNum) {
        this.setX(x);
        this.setY(y);
        this.setWidth(width);
        this.setHeight(height);
        this.setSequenceNum(sequenceNum);
    }

    public void setWidth(Integer width) throws BadRequestException {
        if(width <= 0) {
            throw new BadRequestException("Attempted to make selection of negative or 0 width!");
        }
        this.width = width;
    }

    public void setHeight(Integer height) throws BadRequestException {
        if(height <= 0) {
            throw new BadRequestException("Attempted to make selection of negative or 0 height!");
        }
        this.height = height;
    }

    public void setX(Integer x) throws BadRequestException {
        if(x < 0) {
            throw new BadRequestException("Attempted to make selection of negative x!");
        }
        this.x = x;
    }

    public void setY(Integer y) throws BadRequestException {
        if(y < 0) {
            throw new BadRequestException("Attempted to make selection of negative y!");
        }
        this.y = y;
    }

    public void setSequenceNum(Integer sequenceNum) throws BadRequestException {
        if (sequenceNum < 0) {
            throw new BadRequestException("Attemped to make selection with negative sequence Number!");
        }
        this.sequenceNum = sequenceNum;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getWidth() {
        return this.width;
    }

    public Integer getHeight() {
        return this.height;
    }

    public Integer getSequenceNum() {
        return this.sequenceNum;
    }

    public Integer getMaxX() {
        return this.x + this.width;
    }

    public Integer getMaxY() {
        return this.y + this.height;
    }
}
