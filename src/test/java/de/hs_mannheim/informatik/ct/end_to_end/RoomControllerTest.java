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

import java.net.URLDecoder;
import java.net.URLEncoder;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "allow_full_room_checkIn=true",
        "warning_for_full_room=false"
})
public class RoomControllerTest extends BasicRoomControllerTest{

    @Test
    public void areStaticPagesCallable() throws Exception {
        // strings that called Page should contain
        String rTestRoom = "HSMA CTT - Check-in Raum " + TEST_ROOM_NAME;
        String rTestRoomCheckOut = "Check-out";

        // /{roomId}
        this.mockMvc.perform(
                get("/r/" + TEST_ROOM_NAME).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(rTestRoom)));

        // /{roomId}/checkOut
        this.mockMvc.perform(
                get("/r/" + TEST_ROOM_NAME + "/checkOut").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString(rTestRoomCheckOut)
                ));
    }

    @Test
    public void accessingImportWithoutAdminLogin() throws Exception {
        // /import (should not be accessible without admin login)
        // todo find a way to check for redirect without 'http://localhost'
        this.mockMvc.perform(
                get("/r/import").with(csrf()))
                .andExpect(status().is(302))
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    public void checkInEmptyRoom() throws Exception {
        this.mockMvc.perform(
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void openEventManagerPortal() throws Exception {
        this.mockMvc.perform(
                get("/r/"+TEST_ROOM_NAME+"/event-manager-portal")
                        .param("visitorEmail", TEST_USER_EMAIL)
                )
                .andExpect(status().isOk());
    }

    @Test
    public void resetRoomWithDefaultRedirectURI() throws Exception {
        this.mockMvc.perform(
                post("/r/"+TEST_ROOM_NAME+"/executeRoomReset")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/r/" + TEST_ROOM_NAME + "?&privileged=true"));
    }

    @Test
    public void resetRoomWithCustomRedirectURI() throws Exception {
        String redirectURI = URLEncoder.encode("/r/"+TEST_ROOM_NAME+"/event-manager-portal", "UTF-8");
        this.mockMvc.perform(
                post("/r/"+TEST_ROOM_NAME+"/executeRoomReset")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("redirectURI", redirectURI)
                        .with(csrf())
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URLDecoder.decode(redirectURI, "UTF-8")));
    }

    @Test
    public void checkInFilledRoom() throws Exception {
        // find and fill testroom
        Room testRoom = roomService.findByName(TEST_ROOM_NAME).get();
        fillRoom(testRoom, 5);

        this.mockMvc.perform(
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void checkInFullRoomWithOverride() throws Exception {
        // find and fill testroom
        Room testRoom = roomService.findByName(TEST_ROOM_NAME).get();
        fillRoom(testRoom, 10);

        this.mockMvc.perform(
                get("/r/" + TEST_ROOM_NAME + "?override=true").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl(null));

        this.mockMvc.perform(
                post("/r/checkInOverride")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(forwardedUrl(null))
                .andExpect(status().isOk());
    }

    @Test
    public void checkInInvalidCredentials() throws Exception {
        // check in with empty username should
        this.mockMvc.perform(
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", "")
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Email is invalid")));
    }

    @Test
    public void checkInInvalidRoomPin() throws Exception {
        // check in with empty username should
        this.mockMvc.perform(
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN_INVALID)
                        .with(csrf()))
                .andExpect(status().is(400))
                .andExpect(content().string(containsString("Room pin is invalid")));
    }

    @Test
    public void checkOut() throws Exception {
        // post request on /r/checkout should check out user and redirect to /r/checkedOut
        this.mockMvc.perform(
                // check in
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andDo(
                        // check out
                        result -> mockMvc.perform(
                                post("/r/checkOut")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("visitorEmail", TEST_USER_EMAIL)
                                        .with(csrf()))
                                .andExpect(status().isFound())
                                .andExpect(redirectedUrl("/r/checkedOut")));
    }

    @Test
    public void checkOutInvalidCredentials() throws Exception {
        this.mockMvc.perform(
                // check in
                post("/r/checkIn")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("visitorEmail", TEST_USER_EMAIL)
                        .param("roomId", TEST_ROOM_NAME)
                        .param("roomPin", TEST_ROOM_PIN)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andDo(
                        // check out
                        result -> mockMvc.perform(
                                post("/r/checkOut")
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        // invalid email
                                        .param("visitorEmail", "1" + TEST_USER_EMAIL)
                                        .with(csrf()))
                                .andExpect(status().is(404)));
    }

    @Test
    public void asyncRoomReset() throws Exception {
        this.mockMvc.perform(
                post("/r/" + TEST_ROOM_NAME + "/reset").with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("roomPin", TEST_ROOM_PIN)
        )
                .andExpect(status().is(200));
    }

    @Test
    public void asyncRoomResetWithInvalidPin() throws Exception {
        this.mockMvc.perform(
                post("/r/" + TEST_ROOM_NAME + "/reset").with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("roomPin", TEST_ROOM_PIN_INVALID)
        )
                .andExpect(status().is(400));
    }

    @Test
    public void roomNotFoundException() throws Exception {
        this.mockMvc.perform(
                get("/r/" + "thisRoomShouldNotExsits").with(csrf()))
                .andExpect(status().is(404))  // checking for response status code 404
                .andExpect(content().string(containsString("Room not found")));// checking if error message is displayed for user
    }
}
