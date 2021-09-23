package de.hs_mannheim.informatik.ct.end_to_end;

/*
 * Corona Tracking Tool der Hochschule Mannheim
 * Copyright (C) 2021 Hochschule Mannheim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import de.hs_mannheim.informatik.ct.model.Room;
import de.hs_mannheim.informatik.ct.persistence.InvalidEmailException;
import de.hs_mannheim.informatik.ct.persistence.InvalidExternalUserdataException;
import de.hs_mannheim.informatik.ct.persistence.services.RoomService;
import de.hs_mannheim.informatik.ct.persistence.services.RoomVisitService;
import de.hs_mannheim.informatik.ct.persistence.services.VisitorService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
abstract public class BasicRoomControllerTest {
    @TestConfiguration
    static class RoomControllerTestConfig {
        @Bean
        public RoomService service() {
            return new RoomService();
        }
    }

    @Autowired
    protected RoomService roomService;

    @Autowired
    protected RoomVisitService roomVisitService;

    @Autowired
    protected VisitorService visitorService;

    @Autowired
    protected MockMvc mockMvc;

    protected final String TEST_ROOM_NAME = "123";
    protected String TEST_ROOM_PIN;
    protected final String TEST_ROOM_PIN_INVALID = "";
    protected final String TEST_USER_EMAIL = "1233920@stud.hs-mannheim.de";

    @BeforeEach
    public void setUp() {
        Room room = new Room(TEST_ROOM_NAME, "A", 10);
        TEST_ROOM_PIN = room.getRoomPin();
        roomService.saveRoom(room);
    }

    /**
     * Helper method that creates users to fill room.
     * An address is created by combining iterator value with '@stud.hs-mannheim.de'.
     * To prevent the Test-User getting checked in, 0@stud.hs-mannheim.de is prevented as a fallback Address.
     *
     * @param room   the room that should get filled.
     * @param amount the amount the room will be filled.
     */
    public void fillRoom(Room room, int amount) throws InvalidEmailException, InvalidExternalUserdataException {

        for (int i = 0; i < amount; i++) {
            String randomUserEmail = String.format("%d@stud.hs-mannheim.de", i);

            if (randomUserEmail != TEST_USER_EMAIL) {
                roomVisitService.visitRoom(visitorService.findOrCreateVisitor("" + i + "@stud.hs-mannheim.de", null, null, null), room);
            } else {
                roomVisitService.visitRoom(visitorService.findOrCreateVisitor("0@stud.hs-mannheim.de", null, null, null), room);
            }
        }
    }
}
