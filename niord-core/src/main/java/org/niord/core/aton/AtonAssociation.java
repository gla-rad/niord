package org.niord.core.aton;

import org.niord.core.aton.vo.AtonAssociationVo;
import org.niord.core.model.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An AtoN Association object as per the requirements of the S-125 data model.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
public class AtonAssociation extends BaseEntity<Integer> {

    // Class Variables
    private String associationType;
    @ManyToMany(mappedBy = "associations")
    private Set<AtonNode> peers;

    /** Constructor */
    public AtonAssociation() {

    }

    /** Constructor */
    public AtonAssociation(AtonAssociationVo node) {
        Objects.requireNonNull(node);
        this.setId(node.getId());
        this.associationType = node.getAssociationType();
        if(node.getPeers() != null) {
            setPeers(Arrays.stream(node.getPeers())
                    .map(uid -> new AtonTag(AtonTag.TAG_ATON_UID, uid))
                    .map(uidTag -> {
                        AtonNode ref = new AtonNode();
                        ref.setTags(Collections.singletonList(uidTag));
                        return ref;
                    })
                    .collect(Collectors.toSet()));
        }
    }

    /** Converts this entity to a value object */
    public AtonAssociationVo toVo() {
        AtonAssociationVo vo = new AtonAssociationVo();
        vo.setId(this.getId());
        vo.setAssociationType(this.getAssociationType());
        vo.setPeers(this.peers.stream()
                .map(AtonNode::getAtonUid)
                .toArray(String[]::new));
        return vo;
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    /**
     * Gets association type.
     *
     * @return the association type
     */
    public String getAssociationType() {
        return associationType;
    }

    /**
     * Sets association type.
     *
     * @param associationType the association type
     */
    public void setAssociationType(String associationType) {
        this.associationType = associationType;
    }

    /**
     * Gets peers.
     *
     * @return the peers
     */
    public Set<AtonNode> getPeers() {
        return peers;
    }

    /**
     * Sets peers.
     *
     * @param peers the peers
     */
    public void setPeers(Set<AtonNode> peers) {
        this.peers = peers;
    }
}
