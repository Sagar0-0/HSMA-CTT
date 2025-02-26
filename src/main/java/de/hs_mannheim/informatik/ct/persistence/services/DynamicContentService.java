/*
 * Corona Tracking Tool der Hochschule Mannheim
 * Copyright (c) 2021 Hochschule Mannheim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.hs_mannheim.informatik.ct.persistence.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.zxing.EncodeHintType;

import de.hs_mannheim.informatik.ct.model.Contact;
import de.hs_mannheim.informatik.ct.model.Room;
import de.hs_mannheim.informatik.ct.model.Visitor;
import de.hs_mannheim.informatik.ct.util.ContactListGenerator;
import de.hs_mannheim.informatik.ct.util.DocxTemplate;
import lombok.val;
import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;

@Service
public class DynamicContentService {
    private final Path defaultDocxTemplatePath = FileSystems.getDefault().getPath("templates/printout/room-printout.docx");
    private final Path privilegedDocxTemplatePath = FileSystems.getDefault().getPath("templates/printout/room-printout-privileged.docx");

    public byte[] getQRCodePNGImage(UriComponents uri, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        QRCode.from(uri.toUriString())
                .withSize(width, height)
                .withHint(EncodeHintType.MARGIN, "5")
                .to(ImageType.PNG)
                .writeTo(out);

        return out.toByteArray();
    }

    public void addRoomPrintOutDocx(Room room, boolean privileged, ZipOutputStream outputStream, Function<Room, UriComponents> uriConverter) throws IOException, XmlException {
        XWPFDocument document = getRoomPrintOutDocx(room, uriConverter, privileged);
        writeRoomPrintOutDocx(room, outputStream, document);
    }

    public synchronized void writeRoomPrintOutDocx(Room room, ZipOutputStream outputStream, XWPFDocument document) throws IOException {
        outputStream.putNextEntry(new ZipEntry(room.getBuildingName() + "/" + room.getName() + ".docx"));
        document.write(outputStream);
    }

    public void writeContactList(Collection<Contact<?>> contacts, Visitor target, ContactListGenerator generator, OutputStream outputStream) throws IOException {
        try (val workbook = generator.generateWorkbook(contacts, target.getEmail())) {
            workbook.write(outputStream);
        }
    }

    public XWPFDocument getRoomPrintOutDocx(Room room, Function<Room, UriComponents> uriConverter, boolean privileged) throws IOException, XmlException {
        DocxTemplate templateGenerator;

        DocxTemplate.TextTemplate<Room> textReplacer = (roomContent, templatePlaceholder) -> {
            switch (templatePlaceholder) {
                case "g":
                    return roomContent.getBuildingName();
                case "r":
                    return roomContent.getName();
                case "l":
                    return uriConverter.apply(roomContent).toUriString();
                case "p":
                    return Integer.toString(roomContent.getMaxCapacity());
                case "c":
                    return roomContent.getRoomPin();
                default:
                    throw new UnsupportedOperationException("Template contains invalid placeholder: " + templatePlaceholder);
            }
        };

        Function<Room, byte[]> qrGenerator = roomQr -> {
            // TODO: This is a hack to integrate the PIN code into the QR Code but not in the hyperlink.
            val qrUri = UriComponentsBuilder.fromUri(uriConverter.apply(room).toUri())
                    .queryParam("pin", room.getRoomPin())
                    .build();
            return getQRCodePNGImage(qrUri, 500, 500);
        };

        if (privileged) {
            templateGenerator = new DocxTemplate<>(privilegedDocxTemplatePath.toFile(), textReplacer, qrGenerator);
        } else {
            templateGenerator = new DocxTemplate<>(defaultDocxTemplatePath.toFile(), textReplacer, qrGenerator);
        }

        return templateGenerator.generate(room);
    }
}
