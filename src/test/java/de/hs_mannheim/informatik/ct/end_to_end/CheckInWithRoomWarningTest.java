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
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "allow_full_room_checkIn=false",
        "warning_for_full_room=true"
})
public class CheckInWithRoomWarningTest extends BasicRoomControllerTest{

    @Test
    public void checkInFullRoom() throws Exception {
        // find and fill testroom
        Room testRoom = roomService.findByName(TEST_ROOM_NAME).get();
        fillRoom(testRoom, 20);

        // request form to check into full room should redirect to roomFull/{roomId}
        this.mockMvc.perform(
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("roomFull/" + TEST_ROOM_NAME));
    }
}
