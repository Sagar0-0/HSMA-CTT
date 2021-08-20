package de.hs_mannheim.informatik.ct.controller;

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

import de.hs_mannheim.informatik.ct.model.*;
import de.hs_mannheim.informatik.ct.persistence.EventNotFoundException;
import de.hs_mannheim.informatik.ct.persistence.InvalidEmailException;
import de.hs_mannheim.informatik.ct.persistence.InvalidExternalUserdataException;
import de.hs_mannheim.informatik.ct.persistence.RoomFullException;
import de.hs_mannheim.informatik.ct.persistence.services.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Controller
@Slf4j
public class CtController {
    @Autowired
    private EventService eventService;

    @Autowired
    private EventVisitService eventVisitService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomVisitService roomVisitService;

    @Autowired
    private VisitorService visitorService;

    @Autowired
    private Utilities util;

    @Autowired
    private DynamicContentService contentService;

    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private ContactTracingService contactTracingService;

    @Value("${server.port}")
    private String port;

    @Value("${hostname}")
    private String host;

    @RequestMapping("/")
    public String home(Model model, @CookieValue(value = "checked-in", defaultValue = "false") String checkedIn) {
        model.addAttribute("freeLearnerPlaces", roomVisitService.getRemainingStudyPlaces());
        model.addAttribute("cookie", checkedIn);
//        ResponseCookie springCookie = ResponseCookie.from("user-id", "test")
//                .secure(true)
//                .path("/")
//                .maxAge(3000)
//                .domain("/test")
//                .build();
//
//        ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, springCookie.toString()).build();
//
//        System.out.println("Mein Cookie: " + springCookie);
        return "index";
    }

    @RequestMapping("/neu") // wenn neue veranstaltung erstellt wurde
    public String newEvent(@RequestParam String name, @RequestParam Optional<Integer> max,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date datum,
                           @RequestParam String zeit,    // TODO: schauen, ob das auch eleganter geht
                           Model model, Authentication auth, @RequestHeader(value = "Referer", required = false) String referer) {

        // Optional beim int, um prüfen zu können, ob ein Wert übergeben wurde
        if (name.isEmpty() || !max.isPresent()) {                // Achtung, es gibt Java-8-Versionen, die Optional.isEmpty noch nicht enthalten!
            model.addAttribute("message", "Bitte alle mit Sternchen markierten Felder ausfüllen.");
            return "neue";
        }

        if (referer != null && (referer.endsWith("/neuVer") || referer.contains("neu?name="))) {
            datum = util.uhrzeitAufDatumSetzen(datum, zeit);

            Room defaultRoom = roomService.saveRoom(new Room("test", "test", max.get()));
            Event v = eventService.saveEvent(new Event(name, defaultRoom, datum, auth.getName()));

            return "redirect:/zeige?vid=" + v.getId(); // Qr code zeigen
        }

        return "neue";
    }

//    @RequestMapping("/test")
//    public String readCookie(@CookieValue(value = "username", defaultValue = "Atta") String username) {
//        System.out.println("Hey! My username is " + username);
//    }

    @GetMapping("/cookie-check-in")
    public void setCookieTrue(HttpServletResponse response) {
        // create a cookie
        Cookie cookie = new Cookie("checked-in", "true");

        //add cookie to response
        response.addCookie(cookie);

        System.out.println("changed cookie to checked in");
    }

    @GetMapping("/cookie-check-not")
    public void setCookieFalse(HttpServletResponse response) {
        // create a cookie
        Cookie cookie = new Cookie("checked-in", "test");

        cookie.setMaxAge(10);

        //add cookie to response
        response.addCookie(cookie);

        System.out.println("changed cookie to test");
    }
    @GetMapping("/cookie-delete")
    public void deleteCookie(HttpServletResponse response) {
        // create a cookie
        Cookie cookie = new Cookie("checked-in", "test");
        cookie.setMaxAge(0);

        //add cookie to response
        response.addCookie(cookie);

        System.out.println("changed cookie to test");
    }

    @RequestMapping("/besuch")
    public String neuerBesuch(@RequestParam Long vid, Model model) throws EventNotFoundException {
        Optional<Event> event = eventService.getEventById(vid);

        if (event.isPresent()) {
            model.addAttribute("vid", event.get().getId());
            model.addAttribute("name", event.get().getName());

            return "eintragen";
        }
        throw new EventNotFoundException();
    }

    @RequestMapping("/besuchMitCode")
    public String besuchMitCode(@RequestParam Long vid, @CookieValue(value = "email", required = false) String email, Model model, HttpServletResponse response) throws UnsupportedEncodingException, InvalidEmailException, EventNotFoundException, RoomFullException, InvalidExternalUserdataException {
        if (email == null) {
            model.addAttribute("vid", vid);
            return "eintragen";
        }

        return besucheEintragen(vid, email, true, model, "/besuchMitCode", response);
    }

    @PostMapping("/senden")
    public String besucheEintragen(@RequestParam Long vid, @RequestParam String email, @RequestParam(required = false, defaultValue = "false") boolean saveMail, Model model,
                                   @RequestHeader(value = "Referer", required = false) String referer, HttpServletResponse response) throws UnsupportedEncodingException, InvalidEmailException, EventNotFoundException, RoomFullException, InvalidExternalUserdataException {

        model.addAttribute("vid", vid);

        if (referer != null && (referer.contains("/besuch") || referer.contains("/senden") || referer.contains("/besuchMitCode"))) {
            if (email.isEmpty()) {
                throw new InvalidEmailException();
            } else {
                Optional<Event> event = eventService.getEventById(vid);

                if (!event.isPresent()) {
                    throw new EventNotFoundException();
                } else {
                    int visitorCount = eventService.getVisitorCount(vid);
                    Event v = event.get();

                    if (visitorCount >= v.getRoomCapacity()) {
                        throw new RoomFullException();
                    } else {
                        Visitor b = null;
                        b = visitorService.findOrCreateVisitor(email, null, null, null);

                        Optional<String> autoAbmeldung = Optional.empty();
                        List<EventVisit> nichtAbgemeldeteBesuche = eventVisitService.signOutVisitor(b, dateTimeService.getDateNow());

                        if (nichtAbgemeldeteBesuche.size() > 0) {
                            autoAbmeldung = Optional.ofNullable(nichtAbgemeldeteBesuche.get(0).getEvent().getName());
                            // TODO: Warning in server console falls mehr als eine Event abgemeldet wurde,
                            //  da das eigentlich nicht möglich ist
                        }

                        EventVisit vb = new EventVisit(v, b, dateTimeService.getDateNow());
                        vb = eventService.saveVisit(vb);

                        if (saveMail) {
                            Cookie c = new Cookie("email", email);
                            c.setMaxAge(60 * 60 * 24 * 365 * 5);
                            c.setPath("/");
                            response.addCookie(c);
                        }

                        UriComponents uriComponents = UriComponentsBuilder.newInstance()
                                .path("/angemeldet")
                                .queryParam("email", b.getEmail())
                                .queryParam("veranstaltungId", v.getId())
                                .queryParamIfPresent("autoAbmeldung", autoAbmeldung)
                                .build()
                                .encode(StandardCharsets.UTF_8);

                        return "redirect:" + uriComponents.toUriString();
                    } // endif Platz im Raum

                } // endif Event existiert

            } // endif nicht leere Mail-Adresse

        } // endif referer korrekt?

        return "eintragen";
    }

    @RequestMapping("/veranstaltungen")
    public String eventList(Model model) {
        Collection<Event> events = eventService.getAll();
        model.addAttribute("events", events);

        return "eventList";
    }

    @RequestMapping("/zeige")
    public String showEvent(@RequestParam Long vid, Model model) throws EventNotFoundException {
        Optional<Event> event = eventService.getEventById(vid);

        if (event.isPresent()) {
            model.addAttribute("event", event.get());
            model.addAttribute("teilnehmerzahl", eventService.getVisitorCount(vid));

            return "event";
        }
        throw new EventNotFoundException();
    }


    @RequestMapping("/angemeldet")
    public String angemeldet(
            @RequestParam String email, @RequestParam(value = "veranstaltungId") long eventId,
            @RequestParam(required = false) Optional<String> autoAbmeldung, Model model, HttpServletResponse response) throws EventNotFoundException {

        Optional<Event> v = eventService.getEventById(eventId);

        if (!v.isPresent()) {
            throw new EventNotFoundException();
        }

        model.addAttribute("visitorEmail", email);
        model.addAttribute("autoAbmeldung", autoAbmeldung.orElse(""));

        model.addAttribute("message", "Vielen Dank, Sie wurden erfolgreich im Raum eingecheckt.");

        Cookie c = new Cookie("checked-into", "" + v.get().getId());    // Achtung, Cookies erlauben keine Sonderzeichen (inkl. Whitespaces)!
        c.setMaxAge(60 * 60 * 8);
        c.setPath("/");
        response.addCookie(c);

        return "angemeldet";
    }

    @RequestMapping("/abmelden")
    public String abmelden(@RequestParam(name = "besucherEmail", required = false) String besucherEmail,
                           Model model, HttpServletRequest request, HttpServletResponse response, @CookieValue("email") String mailInCookie) {

        // TODO: ich denke, wir müssen das Speichern der Mail im Cookie zur Pflicht machen, wenn wir den Logout über die Leiste oben machen wollen?
        // Oder wir versuchen es mit einer Session-Variablen?

        if (besucherEmail == null || besucherEmail.length() == 0) {
            if (mailInCookie != null && mailInCookie.length() > 0)
                besucherEmail = mailInCookie;
            else
                return "index";
        }

        visitorService.findVisitorByEmail(besucherEmail)
                .ifPresent(value -> eventVisitService.signOutVisitor(value, dateTimeService.getDateNow()));

        Cookie c = new Cookie("checked-into", "");
        c.setMaxAge(0);
        c.setPath("/");
        response.addCookie(c);

        return "abgemeldet";
    }

    // zum Testen ggf. wieder aktivieren
    //	@RequestMapping("/loeschen")
    //	public String kontakteLoeschen(Model model) {
    //		vservice.loescheAlteBesuche();
    //		model.addAttribute("message", "Alte Kontakte gelöscht!");
    //
    //		return "index";
    //	}

    @RequestMapping("/neuVer")
    public String neu() {
        return "neue";
    }

    @RequestMapping("/login")
    public String login() {
        return "login";
    }

    @RequestMapping("/datenschutz")
    public String datenschutz() {
        return "datenschutz";
    }

    @RequestMapping("/howToQr")
    public String howToQr() {
        return "howToQr";
    }

    @RequestMapping("/howToInkognito")
    public String howToInkognito() {
        return "howToInkognito";
    }

    @RequestMapping("/learningRooms")
    public String showLearningRooms(Model model) {
        model.addAttribute("learningRoomsCapacity", roomVisitService.getAllStudyRooms());
        return "learningRooms";
    }

    @RequestMapping("/faq")
    public String showFaq() {
        return "faq";
    }
}
