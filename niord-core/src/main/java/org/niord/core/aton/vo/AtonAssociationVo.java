package org.niord.core.aton.vo;

import org.niord.model.IJsonSerializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * An AtoN Association Type.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@XmlRootElement(name = "association")
public class AtonAssociationVo implements IJsonSerializable {

    // Class Variables
    private Integer id;
    private String associationType;
    private String[] peers;

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlAttribute
    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType(String associationType) {
        this.associationType = associationType;
    }

    @XmlAttribute
    public String[] getPeers() {
        return peers;
    }

    public void setPeers(String[] peers) {
        this.peers = peers;
    }
}
