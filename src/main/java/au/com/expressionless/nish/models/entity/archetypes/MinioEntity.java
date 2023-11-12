package au.com.expressionless.nish.models.entity.archetypes;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class MinioEntity extends StandardEntity {
    
    @Column(name = "minio_id")
    private String minioId;
    
    public MinioEntity() {/* no-args default const */}

    public String getMinioId() {
        return this.minioId;
    }

    public void setMinioId(String minioId) {
        this.minioId = minioId;
    }
    
}
