import Utils.CustomMapComparator;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class SushiOrderBot extends TelegramLongPollingBot {

    private static Map<Long, Sessione> sessioniAttive = new ConcurrentHashMap<Long, Sessione>();
    private static Map<Long, Long> sessioniInCorso = new ConcurrentHashMap<Long, Long>();
    private static Map<Long, Long> sessioniPassword = new ConcurrentHashMap<Long, Long>();
    private static Map<Long, Stati> statiPerChat = new ConcurrentHashMap<Long, Stati>();
    private static Map<Long, List<String>> piattiPerChat = new ConcurrentHashMap<Long, List<String>>();

    private static final String BOT_NAME = "SushiOrderBot";
    private static final String BOT_TOKEN = "871656793:AAEND2Y809PBlWI6oqEMlVeLwaR-uJHeEzQ";

    private static final String MESSAGE_START = "ciao, sono SushiOrderBot \uD83C\uDF63\nti aiuterò ad aggregare le ordinazioni di tutto il tavolo!\nmettiti d'accordo con gli altri commensali e inserite lo stesso numero di sessione per iniziare";
    private static final String MESSAGE_SESSIONE = "ti sei unito alla sessione\ninizia ad inviarmi i tuoi ordini\ninviami solo il numero! (Per esempio: se nel menù il Nigiri di salmone è il numero 10, inviamo solo il numero 10)\nse ne vuoi più di uno dello stesso tipo, mandami il suo numero più volte\nquando hai finito, utilizza il comando /fine";
    private static final String MESSAGE_ATTENDI_CONFERMA = "Questa è la tua ordinazione\nutilizza il comando /conferma se è tutto ok, altrimenti usa il comando /rimuovi per rimuovere piatti";
    private static final String MESSAGE_ATTENDI = "non tutti i partecipanti sono pronti ad ordinare\nper controllare lo stato delle ordinazioni, utilizza /stato";
    private static final String MESSAGE_ALL_READY = "siamo tutti pronti!\nutilizza il comando /termina per terminare le ordinazioni e ricevere il menù completo";
    private static final String MESSAGE_ANNULLA = "ordine correttamente eliminato\nse vuoi ordinare del Sushi \uD83C\uDF63 utilizza il comando /start";
    private static final String MESSAGE_ATTENDI_FINE_SESSIONE = "devi attendere che tutti abbiano concluso la propria ordinazione prima di chiudere la sessione!\nutilizza il comando  /stato per controllare chi manca";
    private static final String MESSAGE_SESSIONE_CHIUSA = "Enjoy the \uD83C\uDF63!";
    private static final String MESSAGE_NESSUN_PIATTO = "sei a digiuno?\n non hai inviato nessun piatto!\ninviami i numeri dei piatti che vuoi ordinare, non essere timido  \uD83C\uDF63.";
    private static final String MESSAGE_RIMUOVI = "questo è il tuo ordine attuale\nmandami i numeri dei piatti che vuoi eliminare, uno alla volta\nquando hai finito usa il comando /fine";
    private static final String MESSAGE_CREA_PASSWORD = "la sessione è disponibile\nInserisci una password per l'accesso alla sessione e comunicala agli altri commensali";
    private static final String MESSAGE_INSERISCI_PASSWORD = "c'è una sessione in corso, inviami la password per accedervi";
    private static final String MESSAGE_PASSWORD_SBAGLIATA = "la password inserita è errata\ninserisci la password corretta";
    private static final String MESSAGE_ERROR = "utilizza prima un comando!\nincomincia con /start, se sei bloccato usa /annulla ";

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage() != null) {

            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String nickname = update.getMessage().getFrom().getUserName();
            if (nickname.isEmpty()) {
                nickname = update.getMessage().getFrom().getFirstName() + " " + update.getMessage().getFrom().getLastName();
            }

            if (!statiPerChat.containsKey(chatId)) {
                statiPerChat.put(chatId, Stati.start);
            }

            Stati statoAttuale = statiPerChat.get(chatId);

            if (statoAttuale == Stati.start && message.equals("/start")) {
                comandoStart(chatId);
            } else if (message.equals("/annulla")) {
                annulla(chatId);
            } else if (statoAttuale == Stati.sessione && StringUtils.isNumeric(message)) {
                insertSessione(message, chatId, nickname);
            } else if (statoAttuale == Stati.inserisciPassword || statoAttuale == Stati.creaPassword) {
                insertPassword(message, chatId, nickname);
            } else if (statoAttuale == Stati.ordine && (StringUtils.isNumeric(message) || Pattern.matches("\\d*[a-z]", message))) {
                insertOrdini(message, chatId);
            } else if (statoAttuale == Stati.ordine && message.equals("/fine")) {
                comandoFine(chatId);
            } else if (statoAttuale == Stati.ok && message.equals("/stato")) {
                comandoStatoSessione(chatId);
            } else if (statoAttuale == Stati.revisione && message.equals("/conferma")) {
                comandoConferma(chatId);
            } else if (statoAttuale == Stati.revisione && message.equals("/rimuovi")) {
                comandoRimuovi(chatId);
            } else if (statoAttuale == Stati.revisione && (StringUtils.isNumeric(message) || Pattern.matches("\\d*[a-z]", message))) {
                removeOrdini(chatId, message);
            } else if (statoAttuale == Stati.revisione && message.equals("/fine")) {
                comandoFineRevisione(chatId);
            } else if (statoAttuale == Stati.ok && message.equals("/termina")) {
                comandoTerminaSessione(chatId);
            } else if (statoAttuale != Stati.ordine) {
                sendMessage(MESSAGE_ERROR, chatId);
            }
        }
    }

    private void insertPassword(String password, long chatId, String nickname) {

        Ordine nuovoOrdine = creaOrdine(chatId, nickname);
        Long idSessione = sessioniPassword.get(chatId);

        if (statiPerChat.get(chatId).equals(Stati.creaPassword)) {
            sessioniAttive.get(idSessione).setPassword(password);
            sessioniAttive.get(idSessione).getOrdini().add(nuovoOrdine);
            sessioniInCorso.put(chatId, idSessione);
            statiPerChat.put(chatId, Stati.ordine);
            sendMessage(MESSAGE_SESSIONE, chatId);
        } else {
            if ((sessioniAttive.get(idSessione).getPassword().equals(password))) {
                sessioniAttive.get(idSessione).getOrdini().add(nuovoOrdine);
                sessioniInCorso.put(chatId, idSessione);
                statiPerChat.put(chatId, Stati.ordine);
                sendMessage(MESSAGE_SESSIONE, chatId);
            } else {
                sendMessage(MESSAGE_PASSWORD_SBAGLIATA, chatId);
            }
        }
    }

    private void insertSessione(String message, long chatId, String nickname) {

        Long idSessione = Long.parseLong(message);
        sessioniPassword.put(chatId, idSessione);

        if (sessioniAttive.containsKey(idSessione)) {
            statiPerChat.put(chatId, Stati.inserisciPassword);
            sendMessage(MESSAGE_INSERISCI_PASSWORD, chatId);
        } else {
            statiPerChat.put(chatId, Stati.creaPassword);

            Sessione nuovaSessione = new Sessione();
            nuovaSessione.setIdSessione(idSessione);
            sessioniAttive.put(idSessione, nuovaSessione);
            sessioniAttive.get(idSessione).setOrdini(new ArrayList<Ordine>());

            sendMessage(MESSAGE_CREA_PASSWORD, chatId);
        }
    }


    private void comandoConferma(long chatId) {
        Long idSessione = sessioniInCorso.get(chatId);
        List<Long> chatIds = new ArrayList<Long>();

        List<Ordine> ordiniInCorso = sessioniAttive.get(idSessione).getOrdini();
        Ordine[] ordiniArray = ordiniInCorso.toArray(new Ordine[ordiniInCorso.size()]);
        for (int i = 0; i < ordiniArray.length; i++) {
            Ordine ordine = ordiniArray[i];
            if (ordine.getChatId().equals(chatId)) {
                ordine.setReady(true);
            }
        }

        statiPerChat.put(chatId, Stati.ok);
        comandoStatoSessione(chatId);
    }

    private void comandoFineRevisione(long chatId) {

        //TODO SE PIATTI NULL MESSAGGIO ERRORE
        statiPerChat.put(chatId, Stati.ok);
        Long idSessione = sessioniInCorso.get(chatId);
        List<Long> chatIds = new ArrayList<Long>();

        //TODO migliorare usando una mappa
        List<Ordine> ordiniInCorso = sessioniAttive.get(idSessione).getOrdini();
        Ordine[] ordiniArray = ordiniInCorso.toArray(new Ordine[ordiniInCorso.size()]);
        for (int i = 0; i < ordiniArray.length; i++) {
            Ordine ordine = ordiniArray[i];
            if (ordine.getChatId().equals(chatId)) {
                ordine.setPiatti(piattiPerChat.get(chatId));
                ordine.setReady(true);
            }
        }

        if (!piattiPerChat.isEmpty()) {
            sendOrderList(piattiPerChat.get(chatId), chatId);
        } else {
            sendMessage(MESSAGE_NESSUN_PIATTO, chatId);
            statiPerChat.put(chatId, Stati.ordine);
            return;
        }

        boolean attesa = false;
        for (Ordine ordine : sessioniAttive.get(idSessione).getOrdini()) {
            if (!ordine.isReady()) {
                attesa = true;
                break;
            } else {
                chatIds.add(ordine.getChatId());
            }
        }

        if (attesa) {
            sendMessage(MESSAGE_ATTENDI_CONFERMA, chatId);
        } else {
            sendMessage(MESSAGE_ALL_READY, chatIds);
        }
    }

    private void removeOrdini(long chatId, String message) {
        if (piattiPerChat.containsKey(chatId) && piattiPerChat.get(chatId).contains(message)) {
            piattiPerChat.get(chatId).remove(message);
        }
    }

    private void comandoRimuovi(long chatId) {
        if (!piattiPerChat.isEmpty()) {
            sendOrderList(piattiPerChat.get(chatId), chatId);
        } else {
            sendMessage(MESSAGE_NESSUN_PIATTO, chatId);
            statiPerChat.put(chatId, Stati.ordine);
        }
        sendMessage(MESSAGE_RIMUOVI, chatId);
    }

    private void comandoTerminaSessione(long chatId) {
        Long idSessione = sessioniInCorso.get(chatId);

        List<Ordine> listaOrdini = sessioniAttive.get(idSessione).getOrdini();

        List<Ordine> ordiniFinali = new ArrayList<Ordine>();
        List<String> piattiFinali = new ArrayList<String>();

        List<Long> chatIds = new ArrayList<Long>();

        boolean attesa = false;

        Ordine[] ordiniArray = listaOrdini.toArray(new Ordine[listaOrdini.size()]);
        for (int i = 0; i < ordiniArray.length; i++) {
            Ordine ordine = ordiniArray[i];
            if (!ordine.isReady()) {
                attesa = true;
                break;
            } else {
                ordiniFinali.add(ordine);
            }
        }

        if (attesa) {
            sendMessage(MESSAGE_ATTENDI_FINE_SESSIONE, chatId);
        } else {
            for (Ordine ordineFinale : ordiniFinali) {
                piattiFinali.addAll(ordineFinale.getPiatti());
                chatIds.add(ordineFinale.getChatId());
            }
            sendOrderList(piattiFinali, chatIds);
            sendMessage(MESSAGE_SESSIONE_CHIUSA, chatIds);


        }            annulla(chatIds);

    }

    private void comandoStart(long chatId) {
        statiPerChat.put(chatId, Stati.sessione);
        sendMessage(MESSAGE_START, chatId);
    }

    private void sendOrderList(List<String> piatti, long chatId) {
        String response = "\uD83C\uDF63 \uD83C\uDF63 \uD83C\uDF63 IL TUO ORDINE \uD83C\uDF63 \uD83C\uDF63 \uD83C\uDF63\n";

        Map<String, Integer> ordiniFinali = new TreeMap<String, Integer>();
        for (String piatto : piatti) {
            if (ordiniFinali.containsKey(piatto)) {
                Integer valore = ordiniFinali.get(piatto) + 1;
                ordiniFinali.put(piatto, valore);
            } else {
                ordiniFinali.put(piatto, 1);
            }
        }

        TreeMap<String, Integer> ordiniOrdinati = new TreeMap<String, Integer>(new CustomMapComparator());
        ordiniOrdinati.putAll(ordiniFinali);

        for (Map.Entry<String, Integer> entry : ordiniOrdinati.entrySet()) {
            response = response + entry.getKey() + " x" + entry.getValue() + "\n";
        }

        sendMessage(response, chatId);
    }

    private void sendOrderList(List<String> piatti, List<Long> chatIds) {
        String response = "\uD83C\uDF63 \uD83C\uDF63 \uD83C\uDF63 ORDINE DEL TAVOLO \uD83C\uDF63 \uD83C\uDF63 \uD83C\uDF63\n";

        Map<String, Integer> ordiniFinali = new TreeMap<String, Integer>();
        for (String piatto : piatti) {
            if (ordiniFinali.containsKey(piatto)) {
                Integer valore = ordiniFinali.get(piatto) + 1;
                ordiniFinali.put(piatto, valore);
            } else {
                ordiniFinali.put(piatto, 1);
            }
        }

        TreeMap<String, Integer> ordiniOrdinati = new TreeMap<String, Integer>(new CustomMapComparator());
        ordiniOrdinati.putAll(ordiniFinali);

        for (Map.Entry<String, Integer> entry : ordiniOrdinati.entrySet()) {
            response = response + entry.getKey() + " x" + entry.getValue() + "\n";
        }

        for (Long chatId : chatIds) {
            statiPerChat.put(chatId, Stati.ok);
        }
        sendMessage(response, chatIds);
    }

    private Ordine creaOrdine(long chatId, String nickname) {
        Ordine nuovoOrdine = new Ordine();
        nuovoOrdine.setChatId(chatId);
        nuovoOrdine.setReady(false);
        nuovoOrdine.setNickname(nickname);
        nuovoOrdine.setPiatti(new ArrayList<String>());
        return nuovoOrdine;
    }

    private void comandoFine(long chatId) {

        //TODO SE PIATTI NULL MESSAGGIO ERRORE
        statiPerChat.put(chatId, Stati.revisione);
        Long idSessione = sessioniInCorso.get(chatId);
        List<Long> chatIds = new ArrayList<Long>();

        //TODO migliorare usando una mappa
        List<Ordine> ordiniInCorso = sessioniAttive.get(idSessione).getOrdini();
        Ordine[] ordiniArray = ordiniInCorso.toArray(new Ordine[ordiniInCorso.size()]);
        for (int i = 0; i < ordiniArray.length; i++) {
            Ordine ordine = ordiniArray[i];
            if (ordine.getChatId().equals(chatId)) {
                ordine.setPiatti(piattiPerChat.get(chatId));
//                ordine.setReady(true);
            }
        }

        if (!piattiPerChat.isEmpty()) {
            sendOrderList(piattiPerChat.get(chatId), chatId);
        } else {
            sendMessage(MESSAGE_NESSUN_PIATTO, chatId);
            statiPerChat.put(chatId, Stati.ordine);
            return;
        }

        boolean attesa = false;
        for (Ordine ordine : sessioniAttive.get(idSessione).getOrdini()) {
            if (!ordine.isReady()) {
                attesa = true;
                break;
            } else {
                chatIds.add(ordine.getChatId());
            }
        }

//        if(attesa){
//            sendMessage(MESSAGE_ATTENDI_CONFERMA, chatId);
//        } else {
//            sendMessage(MESSAGE_ALL_READY, chatIds);
//        }

        sendMessage(MESSAGE_ATTENDI_CONFERMA, chatId);
    }

    private void insertOrdini(String message, long chatId) {
        if (!piattiPerChat.containsKey(chatId)) {
            piattiPerChat.put(chatId, new ArrayList<String>());
        }

        piattiPerChat.get(chatId).add(message);
    }

    private void comandoStatoSessione(long chatId) {
        Long idSessione = sessioniInCorso.get(chatId);
        List<Ordine> ordini = sessioniAttive.get(idSessione).getOrdini();

        List<String> nicknameNonPronti = new ArrayList<String>();
        for (Ordine ordine : ordini) {
            if (!ordine.isReady()) {
                nicknameNonPronti.add(ordine.getNickname());
            }
        }

        String message = "Utenti non pronti: ";
        if (nicknameNonPronti.isEmpty()) {
            sendMessage(MESSAGE_ALL_READY, chatId);
        } else {
            for (String nickname : nicknameNonPronti) {
                message = message + "\n" + nickname;
            }
            message = message + "\n" + MESSAGE_ATTENDI;
            sendMessage(message, chatId);
        }

    }

    private void annulla(long chatId) {
        if (sessioniInCorso.containsKey(chatId)) {
            Long idSessione = sessioniInCorso.get(chatId);

            if (piattiPerChat.containsKey(chatId))
                piattiPerChat.remove(chatId);
            if (statiPerChat.containsKey(chatId))
                statiPerChat.remove(chatId);
            if (sessioniAttive.containsKey(chatId))
                sessioniAttive.remove(idSessione);
            if (sessioniInCorso.containsKey(chatId))
                sessioniInCorso.remove(chatId);
        }

        sendMessage(MESSAGE_ANNULLA, chatId);
    }

    private void annulla(List<Long> chatIds) {
        for (Long chatId : chatIds) {
            if (sessioniInCorso.containsKey(chatId)) {
                Long idSessione = sessioniInCorso.get(chatId);

                if (piattiPerChat.containsKey(chatId))
                    piattiPerChat.remove(chatId);
                if (statiPerChat.containsKey(chatId))
                    statiPerChat.remove(chatId);
                if (sessioniAttive.containsKey(chatId))
                    sessioniAttive.remove(idSessione);
                if (sessioniInCorso.containsKey(chatId))
                    sessioniInCorso.remove(chatId);
            }
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

    public void sendMessage(String message, List<Long> chatIds) {

        for (long chatId : chatIds) {
            SendMessage sendMessage = new SendMessage().setChatId(chatId).setText(message);
            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                System.out.println("Errore nell'invio del messaggio");
            }
        }
    }

    public void onUpdatesReceived(List<Update> updates) {

        for (Update update : updates) {
            onUpdateReceived(update);
        }
    }

    public String getBotUsername() {
        return BOT_NAME;
    }

    public String getBotToken() {
        return BOT_TOKEN;
    }

    public enum Stati {
        start, sessione, creaPassword, inserisciPassword, ordine, revisione, ok;
    }
}

