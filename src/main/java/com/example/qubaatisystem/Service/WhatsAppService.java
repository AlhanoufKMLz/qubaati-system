package com.example.qubaatisystem.Service;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${twilio.account.sid:}")
    private String accountSid;

    @Value("${twilio.auth.token:}")
    private String authToken;

    @Value("${twilio.whatsapp.number:}")
    private String fromNumber;

    public boolean sendWelcomeMessage(String phoneNumber, String fullName, String roleName) {
        return sendMessage(phoneNumber, welcomeBody(fullName, roleName), "welcome");
    }

    public boolean sendActivityAssignedToParent(String phoneNumber, String parentName, String studentName,
                                                String teacherName, String activityTitle) {
        return sendMessage(
                phoneNumber,
                activityAssignedBody(parentName, studentName, teacherName, activityTitle),
                "activity assignment"
        );
    }

    private boolean sendMessage(String phoneNumber, String body, String purpose) {
        if (isBlank(accountSid) || isBlank(authToken) || isBlank(fromNumber) || isBlank(phoneNumber)) {
            log.warn("WhatsApp {} message skipped because Twilio config or phone number is missing", purpose);
            return false;
        }

        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new PhoneNumber("whatsapp:" + phoneNumber),
                    new PhoneNumber("whatsapp:" + fromNumber),
                    body
            ).create();
            return true;
        } catch (Exception e) {
            log.warn("Unable to send WhatsApp {} message to {}", purpose, phoneNumber, e);
            return false;
        }
    }

    private String welcomeBody(String fullName, String roleName) {
        String name = isBlank(fullName) ? "مستخدم قبعتي" : fullName;
        if ("ولي أمر".equals(roleName)) {
            return "مرحبًا " + name + "،\n\n"
                    + "تم إنشاء حسابك في **قبعتي** بنجاح ✅\n\n"
                    + "يمكنك الآن متابعة رحلة طفلك التعليمية والاطلاع على مهاراته وتقدمه والتوصيات المخصصة له.\n\n"
                    + "شكرًا لانضمامك إلى قبعتي 🌟";
        }

        return "مرحبًا " + name + "،\n\n"
                + "تم إنشاء حسابك في **قبعتي** بنجاح ✅\n\n"
                + "يمكنك الآن إدارة فصولك الدراسية، متابعة تقدم الطلاب، والاستفادة من التحليلات والتوصيات الذكية لدعم رحلتهم التعليمية.\n\n"
                + "شكرًا لانضمامك إلى قبعتي 🌟";
    }

    private String activityAssignedBody(String parentName, String studentName, String teacherName, String activityTitle) {
        String parent = isBlank(parentName) ? "ولي الأمر" : parentName;
        String student = isBlank(studentName) ? "طفلك" : studentName;
        String teacher = isBlank(teacherName) ? "المعلم" : teacherName;
        String activity = isBlank(activityTitle) ? "نشاط جديد" : activityTitle;
        return "مرحبًا " + parent + "،\n\n"
                + "تم إسناد نشاط جديد للطالب " + student + " في **قبعتي** ✅\n\n"
                + "النشاط: " + activity + "\n"
                + "بواسطة: " + teacher + "\n\n"
                + "يمكنك متابعة تقدم طفلك والاطلاع على أدائه من حساب ولي الأمر.";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
