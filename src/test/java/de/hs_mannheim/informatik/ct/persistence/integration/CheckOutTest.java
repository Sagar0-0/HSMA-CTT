package de.hs_mannheim.informatik.ct.persistence.integration;



import de.hs_mannheim.informatik.ct.model.Room;
import de.hs_mannheim.informatik.ct.model.RoomVisit;
import de.hs_mannheim.informatik.ct.model.Visitor;
import de.hs_mannheim.informatik.ct.persistence.RoomVisitHelper;
import de.hs_mannheim.informatik.ct.persistence.repositories.RoomVisitRepository;
import de.hs_mannheim.informatik.ct.persistence.repositories.VisitorRepository;
import de.hs_mannheim.informatik.ct.persistence.services.DateTimeService;
import de.hs_mannheim.informatik.ct.persistence.services.EventVisitService;
import de.hs_mannheim.informatik.ct.persistence.services.RoomVisitService;
import de.hs_mannheim.informatik.ct.util.ScheduledMaintenanceTasks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.junit.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
public class CheckOutTest {
    @TestConfiguration
    static class RoomVisitServiceTestContextConfig {
        @Bean
        public RoomVisitService service() {
            return new RoomVisitService();
        }

        @Bean
        public DateTimeService dateTimeService() {
            return new DateTimeService();
        }

        @Bean
        public EventVisitService eventVisitService() {return new EventVisitService();}

        @Bean
        public ScheduledMaintenanceTasks scheduledMaintenanceTasks() {return new ScheduledMaintenanceTasks();}
    }

    @Autowired
    private RoomVisitService roomVisitService;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomVisitRepository roomVisitRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private ScheduledMaintenanceTasks scheduledMaintenanceTasks;

    @Test
    void dailyForcedCheckOut() {
        Room room = new Room("Test", "Test", 20);
        RoomVisitHelper roomVisitHelper = new RoomVisitHelper(entityManager.persist(room));
        Visitor expiredVisitor = entityManager.persist(new Visitor("expired"));
        Visitor notExpiredVisitor = entityManager.persist(new Visitor("not-expired"));
        Visitor almostExpiredVisitor = entityManager.persist(new Visitor("some-expired"));

        List<RoomVisit> roomVisits = roomVisitHelper.generateExpirationTestData(expiredVisitor, notExpiredVisitor);

        // visitor that checked in yesterday
        roomVisits.add(roomVisitHelper.generateVisit(
                expiredVisitor,
                LocalDateTime.now().minusDays(1).withHour(12),
                null
        ));

        // visitor that checked in almost today
        roomVisits.add(roomVisitHelper.generateVisit(
                almostExpiredVisitor,
                LocalDateTime.now().minusDays(1).withHour(23).withMinute(59),
                null
        ));

        // visitor that checked in today
        roomVisits.add(roomVisitHelper.generateVisit(
                notExpiredVisitor,
                LocalDateTime.now().withHour(12),
                null
        ));

        roomVisitRepository.saveAll(roomVisits);

        scheduledMaintenanceTasks.signOutAllVisitors(LocalTime.of(0, 0));

        assertThat(roomVisitRepository.getRoomVisitorCount(room), equalTo(1));
    }
}
