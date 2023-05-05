package org.niord.core.aton;

import org.niord.core.aton.vo.AtonAggregationVo;
import org.niord.core.model.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An AtoN Aggregation object as per the requirements of the S-125 data model.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Entity
public class AtonAggregation extends BaseEntity<Integer> {

    // Class Variables
    private String aggregationType;
    @ManyToMany(mappedBy = "aggregations")
    private Set<AtonNode> peers;

    /** Constructor */
    public AtonAggregation() {

    }

    /** Constructor */
    public AtonAggregation(AtonAggregationVo node) {
        Objects.requireNonNull(node);
        this.setId(node.getId());
        this.aggregationType = node.getAggregationType();
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
    public AtonAggregationVo toVo() {
        AtonAggregationVo vo = new AtonAggregationVo();
        vo.setId(this.getId());
        vo.setAggregationType(this.getAggregationType());
        vo.setPeers(this.peers.stream()
                .map(AtonNode::getAtonUid)
                .toArray(String[]::new));
        return vo;
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    /**
     * Gets aggregation type.
     *
     * @return the aggregation type
     */
    public String getAggregationType() {
        return aggregationType;
    }

    /**
     * Sets aggregation type.
     *
     * @param aggregationType the aggregation type
     */
    public void setAggregationType(String aggregationType) {
        this.aggregationType = aggregationType;
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
