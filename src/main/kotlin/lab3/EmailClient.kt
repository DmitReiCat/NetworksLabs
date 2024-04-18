package lab3

import lab3.EmailConfig.EMAIL_PASSWORD
import lab3.EmailConfig.EMAIL_USERNAME
import lab3.EmailConfig.SMTP_HOST
import lab3.EmailConfig.SMTP_PORT
import java.io.IOException
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

object EmailConfig {
    const val SMTP_HOST = "smtp.mail.ru"
    const val SMTP_PORT = 465
    const val EMAIL_USERNAME = "xxxxxxxxxxxxxxx@mail.ru"
    const val EMAIL_PASSWORD = "xxxxxxxxxxxxxxx"
}

fun main() {
    val client = EmailClient(SMTP_HOST, SMTP_PORT)
    client.sendMessage(
        username = EMAIL_USERNAME,
        password = EMAIL_PASSWORD,
        subject = "Привет! Это точно не спам! Тыкни на меня!",
        body = "Извольте показать вам юморительную картиночку",
        recipient = "xxxxxxxxxxxxxxx"
    )
}

class EmailClient (
    private val host: String,
    private val port: Int,
) {
    fun sendMessage(
        username: String,
        password: String,
        subject: String,
        body: String,
        recipient: String
    ) {
        val authenticator: Authenticator =
            object : Authenticator() {
                override fun getPasswordAuthentication() = PasswordAuthentication(username, password)
            }

        val session: Session = Session.getInstance(getProperties(host, port.toString()), authenticator)

        try {
            val message =
                MimeMessage(session).apply {
                    setSubject(subject)
                    setFrom(InternetAddress(username))
                    setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipient))
                }

            val textPart = MimeBodyPart()
            textPart.setText(body)

            val imagePath = "src/main/kotlin/lab3/lustige-tiere-186.jpg"

            val imagePart = MimeBodyPart()
            imagePart.attachFile(imagePath)

            val multipart: Multipart = MimeMultipart()
            multipart.addBodyPart(textPart)
            multipart.addBodyPart(imagePart)

            message.setContent(multipart)

            Transport.send(message)

            println("Mail successfully sent.")
        } catch (e: MessagingException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getProperties(
        smtpHost: String,
        smtpPort: String,
    ): Properties {
        val propList =
            listOf(
                "mail.smtp.auth" to "true",
                "mail.smtp.starttls.enable" to "true",
                "mail.smtp.host" to smtpHost,
                "mail.smtp.port" to smtpPort,
                "mail.smtp.socketFactory.port" to smtpPort,
                "mail.smtp.socketFactory.class" to "javax.net.ssl.SSLSocketFactory",
                "mail.smtp.socketFactory.fallback" to "false",
            )
        val properties = Properties().apply { putAll(propList) }
        return properties
    }
}
