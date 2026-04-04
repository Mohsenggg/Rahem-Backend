package com.mgh.backend.invitation.config;

import com.mgh.backend.invitation.domain.entity.InvitationQuestion;
import com.mgh.backend.invitation.repository.InvitationQuestionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class InvitationQuestionDataLoader {

    @Bean
    CommandLineRunner seedInvitationQuestions(InvitationQuestionRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.saveAll(List.of(
                    InvitationQuestion.builder()
                            .question("ما هو لون السماء في يوم صافٍ؟")
                            .options(List.of("أحمر", "أزرق", "أخضر", "أصفر"))
                            .correctAnswer("أزرق")
                            .build(),
                    InvitationQuestion.builder()
                            .question("كم عدد أرجل العنكبوت؟")
                            .options(List.of("٦", "٨", "١٠", "٤"))
                            .correctAnswer("٨")
                            .build(),
                    InvitationQuestion.builder()
                            .question("ما عاصمة المملكة العربية السعودية؟")
                            .options(List.of("جدة", "الرياض", "مكة", "الدمام"))
                            .correctAnswer("الرياض")
                            .build(),
                    InvitationQuestion.builder()
                            .question("أيُّ الكواكب يُعرف بالكوكب الأحمر؟")
                            .options(List.of("الزهرة", "المريخ", "زحل", "عطارد"))
                            .correctAnswer("المريخ")
                            .build()
            ));
        };
    }
}
