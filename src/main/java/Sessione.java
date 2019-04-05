import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class Sessione {

    private Long idSessione;
    private List<Ordine> ordini;

    public Long getIdSessione() {
        return idSessione;
    }

    public void setIdSessione(Long idSessione) {
        this.idSessione = idSessione;
    }

    public List<Ordine> getOrdini() {
        return ordini;
    }

    public void setOrdini(List<Ordine> ordini) {
        this.ordini = ordini;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Sessione)) return false;

        Sessione sessione = (Sessione) o;

        return new EqualsBuilder()
                .append(idSessione, sessione.idSessione)
                .append(ordini, sessione.ordini)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(idSessione)
                .append(ordini)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Sessione{" +
                "idSessione=" + idSessione +
                ", ordini=" + ordini +
                '}';
    }
}
