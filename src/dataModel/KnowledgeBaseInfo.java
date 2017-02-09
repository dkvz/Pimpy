
package dataModel;

import java.util.Date;

/**
 *
 * @author Alain
 */
public class KnowledgeBaseInfo {

    private String author;
    private String description;
    private Date created = null;

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @param author the author to set
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    public void setCreated(long created) {
        // Set the date using regular timestamp.
        // Convert to miliseconds :
        long stamp = created * 1000;
        this.created = new Date(stamp);
    }

    /**
     * @param created the created to set
     */
    public void setCreated(Date created) {
        this.created = created;
    }



}
