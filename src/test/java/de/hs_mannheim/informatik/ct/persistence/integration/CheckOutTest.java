package de.hs_mannheim.informatik.ct.persistence.integration;



import de.hs_mannheim.informatik.ct.model.CheckOutSource;
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
import de.hs_mannheim.informatik.ct.util.TimeUtil;
<<<<<<< HEAD
import org.hamcrest.MatcherAssert;
=======
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.support.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
<<<<<<< HEAD
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
=======
import java.util.List;

>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
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

<<<<<<< HEAD
    private LocalDateTime now = LocalDateTime.now();

    @Test
    void checkout(){
        RoomVisit roomVisit = new RoomVisit(
                new Room("room", "A", 1),
                null,
                TimeUtil.convertToDate(now.minusDays(1)),
                null,
                new Visitor("checkout"),
                CheckOutSource.NotCheckedOut
        );

        roomVisit.checkOut(TimeUtil.convertToDate(now), CheckOutSource.RoomReset);

        MatcherAssert.assertThat(roomVisit.getEndDate(), notNullValue());
        MatcherAssert.assertThat(roomVisit.getCheckOutSource(), not(CheckOutSource.NotCheckedOut));
    }

=======
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
    @Test
    void recurringForceCheckOut_midnight(){
        Room room = new Room("Test", "Test", 20);
        RoomVisitHelper roomVisitHelper = new RoomVisitHelper(entityManager.persist(room));
        Visitor expiredVisitor = entityManager.persist(new Visitor("expired"));

        RoomVisit roomVisit = new RoomVisit(
                room,
                null,
<<<<<<< HEAD
                TimeUtil.convertToDate(now.minusDays(1).withHour(12)),
=======
                TimeUtil.convertToDate(LocalDateTime.now().minusDays(1).withHour(12)),
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
                null,
                expiredVisitor,
                CheckOutSource.NotCheckedOut
                );

        roomVisitRepository.save(roomVisit);
        // forcing check out at 00:00
        scheduledMaintenanceTasks.signOutAllVisitors(LocalTime.of(0, 0));

        assertThat(roomVisitRepository.getRoomVisitorCount(room), is(0));
        assertThat(roomVisit.getCheckOutSource(), is(CheckOutSource.AutomaticCheckout));
    }

    @Test
    void recurringForceCheckOut_morning(){
        Room room = new Room("Test", "Test", 20);
        RoomVisitHelper roomVisitHelper = new RoomVisitHelper(entityManager.persist(room));
        Visitor expiredVisitor = entityManager.persist(new Visitor("expired"));

        RoomVisit roomVisit = new RoomVisit(
                room,
                null,
<<<<<<< HEAD
                TimeUtil.convertToDate(now.withHour(1)),
=======
                TimeUtil.convertToDate(LocalDateTime.now().withHour(1)),
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
                null,
                expiredVisitor,
                CheckOutSource.NotCheckedOut
        );

        roomVisitRepository.save(roomVisit);
        // forcing check out at 03:55
        scheduledMaintenanceTasks.signOutAllVisitors(LocalTime.of(3, 55));

        assertThat(roomVisitRepository.getRoomVisitorCount(room), is(0));
        assertThat(roomVisit.getCheckOutSource(), is(CheckOutSource.AutomaticCheckout));
    }

    @Test
    void dailyForcedCheckOut() {
        Room room = new Room("Test", "Test", 20);
        RoomVisitHelper roomVisitHelper = new RoomVisitHelper(entityManager.persist(room));
        Visitor expiredVisitor = entityManager.persist(new Visitor("expired"));
        Visitor notExpiredVisitor = entityManager.persist(new Visitor("not-expired"));
        Visitor almostExpiredVisitor = entityManager.persist(new Visitor("some-expired"));

        List<RoomVisit> roomVisits = new ArrayList<RoomVisit>();
        // visitor that checked in yesterday
        // getting checked out
        roomVisits.add(roomVisitHelper.generateVisit(
                expiredVisitor,
<<<<<<< HEAD
                now.minusDays(1).withHour(12),
=======
                LocalDateTime.now().minusDays(1).withHour(12),
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
                null
        ));

        // visitor that checked in almost today
        // not getting checked out
        roomVisits.add(roomVisitHelper.generateVisit(
                almostExpiredVisitor,
<<<<<<< HEAD
                now.minusDays(1).withHour(23).withMinute(59),
=======
                LocalDateTime.now().minusDays(1).withHour(23).withMinute(59),
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
                null
        ));

        // visitor that checked in today
        // not getting checked out
        roomVisits.add(roomVisitHelper.generateVisit(
                notExpiredVisitor,
<<<<<<< HEAD
                now.withHour(12),
=======
                LocalDateTime.now().withHour(12),
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
                null
        ));

        roomVisitRepository.saveAll(roomVisits);
        scheduledMaintenanceTasks.signOutAllVisitors(LocalTime.of(0, 0));

        assertThat(roomVisitRepository.getRoomVisitorCount(room), equalTo(1));
        // this filters the one not expired user and checks if he is for sure not checked out.
        // The other visitors can be checked with same parameters.
        for(RoomVisit rv : roomVisits) {
            if (rv.getVisitor() == notExpiredVisitor) {
                assertThat(rv.getCheckOutSource(), is(CheckOutSource.NotCheckedOut));
                assertThat(rv.getEndDate(), is(nullValue()));
            } else {
                assertThat(rv.getCheckOutSource(), is(CheckOutSource.AutomaticCheckout));
<<<<<<< HEAD
                assertThat(rv.getEndDate(), lessThan(TimeUtil.convertToDate(now)));
            }
        }
    }


=======
                assertThat(rv.getEndDate(), lessThan(TimeUtil.convertToDate(LocalDateTime.now())));
            }
        }
    }
>>>>>>> 6bf1f441f45546efc46ae0aafee2c2043a0b32ad
}
