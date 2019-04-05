import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SushiOrderBot extends TelegramLongPollingBot {

    public static Map<Long, Sessione> sessioniAttive = new HashMap<Long, Sessione>();
    public static Map<Long, Long> sessioniInCorso = new HashMap<Long, Long>();

    private static final String BOT_NAME = "SushiOrderBot";
    private static final String BOT_TOKEN = "871656793:AAEND2Y809PBlWI6oqEMlVeLwaR-uJHeEzQ";

    private static final String MESSAGE_START = "Ciao, sono SushiOrderBot \uD83C\uDF63. \n Ti aiuterò a prendere le ordinazioni! Mettiti d'accordo con gli altri commensali e inserite lo stesso numero di Sessione per iniziare!";
    private static final String MESSAGE_SESSIONE = "Perfetto, ti sei unito alla sessione. Ora puoi iniziare ad inviarmi i piatti che vuoi ordinare. Se ne vuoi più di uno dello stesso tipo, mandamelo più volte! \n Quando hai finito, utilizza il comando \"fine\"";
    private static final String MESSAGE_ATTENDI = "Questa è la tua ordinazione, attendi che tutti abbiano concluso! \n Per controllare lo stato delle ordinazioni, utilizza \"/statoSessione\"";


    private static final String MESSAGE_HELP = "";
    private static final String MESSAGE_ERROR = "Utilizza prima un comando! utilizza \"/start\" ";
    private static final String MESSAGE_ULTIMO_ARRIVATO = "Questa è la tua ordinazione, sei l'ultimo! \n Per concludere, utilizza \"/terminaSessione\"";
    private static final String MESSAGE_SESSION_ERROR = "Questa Sessione non esiste! utilizza \"/sessione\" per crearne una, oppure \"/help\" per ricevere aiuto";

    Stati statoAttuale = Stati.start;

    List<String> piatti = new ArrayList<String>();

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {

            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String nickname = update.getMessage().getAuthorSignature();

            if (statoAttuale == Stati.start && message.equals("/start")) {
                comandoStart(chatId);
            } else if (statoAttuale == Stati.sessione && StringUtils.isNumeric(message)) {
                insertSessione(message, chatId, nickname);
            } else if (statoAttuale == Stati.ordine && (StringUtils.isNumeric(message) || Pattern.matches("\\d*[a-z]", message))) {
                insertOrdini(message, chatId);
            } else if (statoAttuale == Stati.ordine && message.equals("/fine")) {
                comandoFine(chatId);
            }
        }
    }

    private void comandoFine(long chatId) {
        statoAttuale = Stati.revisione;
        Long idSessione = sessioniInCorso.get(chatId);

        //TODO migliorare usando una mappa
        List<Ordine> ordiniInCorso = sessioniAttive.get(idSessione).getOrdini();
        for (Ordine ordine : ordiniInCorso) {
            if (ordine.getChatId().equals(chatId)) {
                ordine.setPiatti(piatti);
            }
        }
        sendMessage(MESSAGE_ATTENDI,chatId);
    }

    private void insertOrdini(String message, long chatId) {

    }

    private void comandoStart(long chatId) {
        statoAttuale = Stati.sessione;
        sendMessage(MESSAGE_START, chatId);
    }

    private void insertSessione(String message, long chatId, String nickname) {
        statoAttuale = Stati.ordine;
        Long idSessione = Long.parseLong(message);
        Ordine nuovoOrdine = creaOrdine(chatId, nickname);

        //controllo se sessione esiste
        //TODO controllo presenza ordine precedente in stessa sessione
        if (sessioniAttive.containsKey(idSessione)) {
            //se esiste, mi aggiungo
            sessioniAttive.get(idSessione).getOrdini().add(nuovoOrdine);
        } else {
            //se non esiste, la creo e mi aggiungo
            Sessione nuovaSessione = new Sessione();
            nuovaSessione.setIdSessione(idSessione);
            nuovaSessione.setOrdini(new ArrayList());
            nuovaSessione.getOrdini().add(nuovoOrdine);
            //ora l'ordine è registrato, devo ricordarmi di aggiornarlo quando ho terminato l'ordinazione
        }

        //TODO controllare se già esistente, non so
        sessioniInCorso.put(chatId, idSessione);
        sendMessage(MESSAGE_SESSIONE, chatId);
    }

    private Ordine creaOrdine(long chatId, String nickname) {
        Ordine nuovoOrdine = new Ordine();
        nuovoOrdine.setChatId(chatId);
        nuovoOrdine.setReady(false);
        nuovoOrdine.setNickname(nickname);
        nuovoOrdine.setPiatti(new ArrayList<String>());
        return nuovoOrdine;
    }

    public void onUpdatesReceived(List<Update> updates) {

        for (Update update : updates) {
            onUpdateReceived(update);
        }
    }

    public void sendMessage(String message, long chatId) {
        SendMessage sendMessage = new SendMessage().setChatId(chatId).setText(message);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            System.out.println("Errore nell'invio del messaggio");
        }
    }

    public String getBotUsername() {
        return BOT_NAME;
    }

    public String getBotToken() {
        return BOT_TOKEN;
    }

    public enum Stati {
        start, sessione, ordine, revisione, ok;
    }
}

