import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

public class Ordine {
    private Long chatId;
    private String nickname;
    private List<String> piatti;
    private boolean isReady;

    @Override
    public String toString() {
        return "Ordine{" +
                "chatId=" + chatId +
                ", nickname='" + nickname + '\'' +
                ", piatti=" + piatti +
                ", isReady=" + isReady +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof Ordine)) return false;

        Ordine ordine = (Ordine) o;

        return new EqualsBuilder()
                .append(isReady(), ordine.isReady())
                .append(getChatId(), ordine.getChatId())
                .append(getNickname(), ordine.getNickname())
                .append(getPiatti(), ordine.getPiatti())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getChatId())
                .append(getNickname())
                .append(getPiatti())
                .append(isReady())
                .toHashCode();
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public List<String> getPiatti() {
        return piatti;
    }

    public void setPiatti(List<String> piatti) {
        this.piatti = piatti;
    }

}
