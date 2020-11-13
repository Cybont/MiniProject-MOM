package space.cybon.demo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

@SpringBootApplication
public class DemoApplication {
    private final static String QUEUE_NAME = "mail";
    // non-durable, exclusive, auto-delete queue with an automatically generated name
    private static String queueName = QUEUE_NAME;
    private final static String EXCHANGE_NAME = "mail-exchange";
    // routingKey is a composite topic of three words, concatenated with '.' given as args[1]
    private static String routingKey = "gendered-mails";
    private static String message;
    private static ArrayList<String> mails = new ArrayList<>();

    public static void main(String[] args) throws Exception
    {
        SpringApplication.run(DemoApplication.class, args);
        // args[0]-message; args[1]-topics
        //if (args.length > 0) message = args[0];
        //if (args.length > 1) routingKey = args[1];

        constructMails();
        createQueue(mails, routingKey);
        System.out.println(" [4] Sent routing key '" + routingKey + "' and message '" + message + "'");
    }

    public static void createQueue(ArrayList<String> mails, String rKey) throws Exception
    {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel())
        {
            // queueName = channel.queueDeclare().getQueue();
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // channel.queueDeclare(queueName, false, false, false, null);
            // bind Exchange to queue
            // channel.queueBind(queueName, EXCHANGE_NAME, "");
            channel.basicPublish(EXCHANGE_NAME, rKey, null, message.getBytes("UTF-8"));
        }
    }

    public static void constructMails()
    {
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new FileReader("./src/main/resources/people.json"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder jsonString = new StringBuilder();
        String line = null;

        while (true)
        {
            try {
                if (!((line = br.readLine()) != null)) break;
            } catch (IOException e) {
                e.printStackTrace();
            }
            jsonString.append(line);
        }

        Gson gson = new Gson();

        Type collectionType = new TypeToken<Collection<Person>>(){}.getType();
        ArrayList<Person> people = gson.fromJson(jsonString.toString(), collectionType);

        for (Person m:people)
        {
            message = "Dear";
            if (m.gender.equals("male")) {
                message = message + " Mr " + m.name;
                mails.add(message);
            }
            else {
                message = message + " Ms " + m.name;
                mails.add(message);
            }
        }
    }
}
