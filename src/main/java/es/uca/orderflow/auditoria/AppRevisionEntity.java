// es.uca.orderflow.audit.AppRevisionEntity.java
package es.uca.orderflow.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.DefaultRevisionEntity;

@Entity
@Table(name = "REVINFO")
public class AppRevisionEntity extends DefaultRevisionEntity {
    @Column(name = "USER", length = 120)
    private String user;

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
}
