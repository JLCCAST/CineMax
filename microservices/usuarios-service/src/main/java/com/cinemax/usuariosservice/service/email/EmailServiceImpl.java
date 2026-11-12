package com.cinemax.usuariosservice.service.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Sin remitente explícito, JavaMail inventa uno con el nombre del contenedor.
    @Value("${app.mail-from:CineMax <no-responder@cinemax.local>}")
    private String remitente;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitente);
        message.setTo(toEmail);
        message.setSubject("CineMax - Recuperación de Contraseña");
        
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        message.setText("Hola,\n\nHas solicitado restablecer tu contraseña. "
                + "Haz clic en el siguiente enlace para crear una nueva:\n\n"
                + resetLink + "\n\n"
                + "Si no solicitaste este cambio, ignora este correo.\n\n"
                + "El equipo de CineMax");

        mailSender.send(message);
    }
}