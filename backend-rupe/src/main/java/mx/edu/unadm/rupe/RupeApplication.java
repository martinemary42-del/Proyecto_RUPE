package mx.edu.unadm.rupe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RupeApplication {

    public static void main(String[] args) {
        SpringApplication.run(RupeApplication.class, args);
    }
}
