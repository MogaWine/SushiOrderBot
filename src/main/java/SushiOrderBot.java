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

    private static final String BOT_NAME = "SushiOrderBot";
    private static final String BOT_TOKEN = "871656793:AAEND2Y809PBlWI6oqEMlVeLwaR-uJHeEzQ";

    private static final String MESSAGE_START = "Ciao, sono SushiOrderBot \uD83C\uDF63. \n Ti aiuterò a prendere le ordinazioni! Mettiti d'accordo con gli altri commensali e inserite lo stesso numero di Sessione per iniziare!";
    private static final String MESSAGE_SESSIONE = "Perfetto, ti sei unito alla sessione. Ora puoi iniziare ad inviarmi i piatti che vuoi ordinare. Se ne vuoi più di uno dello stesso tipo, mandamelo più volte!";

    private static final String MESSAGE_HELP = "";
    private static final String MESSAGE_ERROR = "Utilizza prima un comando! utilizza \"/start\" ";
    private static final String MESSAGE_ATTENDI = "Questa è la tua ordinazione, attendi che tutti abbiano concluso! Per concludere, utilizza \"/terminaSessione\"";
    private static final String MESSAGE_SESSION_ERROR = "Questa Sessione non esiste! utilizza \"/sessione\" per crearne una, oppure \"/help\" per ricevere aiuto";

    Stati statoAttuale = Stati.start;

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {

            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (statoAttuale == Stati.start && message.equals("/start")) {
                comandoStart(message, chatId);
            } else if(statoAttuale == Stati.sessione && StringUtils.isNumeric(message)){
                insertSessione(message, chatId);
            }
        }
    }

    private void insertSessione(String message, long chatId) {
        statoAttuale = Stati.ordine;

        //controllo se sessione esiste

        //se esiste, mi aggiungo

        //se non esiste, la creo e mi aggiungo

        sendMessage(MESSAGE_SESSIONE, chatId);
    }

    private void comandoStart(String message, long chatId) {
        statoAttuale = Stati.sessione;
        sendMessage(MESSAGE_START, chatId);
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

    public static class Sessioni{
        public static Map<Long, Sessione> sessioniAttive = new HashMap<Long, Sessione>();
    }

    public enum Stati{
        start, sessione, ordine, revisione, ok;
    }
}

