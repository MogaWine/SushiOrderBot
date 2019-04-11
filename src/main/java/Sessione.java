import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class Sessione {

    private Long idSessione;
    private List<Ordine> ordini;
    private String password;

    @Override
    public String toString() {
        return "Sessione{" +
                "idSessione=" + idSessione +
                ", ordini=" + ordini +
                ", password='" + password + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Sessione)) return false;

        Sessione sessione = (Sessione) o;

        return new EqualsBuilder()
                .append(getIdSessione(), sessione.getIdSessione())
                .append(getOrdini(), sessione.getOrdini())
                .append(getPassword(), sessione.getPassword())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getIdSessione())
                .append(getOrdini())
                .append(getPassword())
                .toHashCode();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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

}
