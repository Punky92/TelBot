package ru.example.SpringDemoBot.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.example.SpringDemoBot.config.BotConfig;
import ru.example.SpringDemoBot.model.User;
import ru.example.SpringDemoBot.repositories.UserRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final UserRepository userRepository;
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";

    private static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type '/start' to see a welcome message\n\n" +
            "Type '/mydata' to see data stored about yourself\n\n" +
            "Type '/help' to see this message again";
    private final BotConfig config;

    @Autowired
    public TelegramBot(UserRepository userRepository, BotConfig config) {
        this.userRepository = userRepository;
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "получить данные"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    public String getBotToken() {
        return  config.getToken();
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
                String textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                List<User> users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {

                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;

                    case "/register":
                        register(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Sorry, command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                String text = "You pressed YES button";
                executeEditMessageText(chatId, text, messageId);
            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "You pressed NO button";
                executeEditMessageText(chatId, text, messageId);
            }
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    @Override
    public String getBotUsername() {
       return config.getBotName();
    }

    private void startCommandReceived(long chatId, String name) {
        //String answer = "Hi, " + name + ", nice to meet you!";
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + " :blush:");
        log.info("Replied to User " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }
    private void executeEditMessageText(long chatId, String text, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
}
