package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.Donation;
import com.francomartinez.donationsystem.models.requests.CancelAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.MarkAttendanceRequest;
import com.francomartinez.donationsystem.models.requests.RescheduleAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.UpdateAppointmentRequest;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Lifecycle coverage for the "Agenda robusta" P1 work: create/update/cancel/
 * reschedule/mark-attendance, the donation link, and the status filter.
 */
public class AppointmentsLifecycleIntegrationTest extends BaseApiTest {

    private int seedDonor() throws Exception {
        int donorId = TestDatabase.insertDonor("44444444", "Lucia", "Suarez", "O+", null);
        TestDatabase.insertDonorPhone(donorId, "3515555555", true);
        return donorId;
    }

    private String futureDate(int days) {
        return LocalDate.now().plusDays(days).toString();
    }

    @Test
    public void createAppointmentPersistsScheduledTurn() throws Exception {
        int donorId = seedDonor();
        String date = futureDate(3);

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().createAppointment(
                new CreateAppointmentRequest(donorId, date, "09:30", "PLASMA")));
        assertTrue(response.isSuccess());

        int id = TestDatabase.queryInt("SELECT id FROM appointments WHERE donor_id = " + donorId);
        assertEquals("SCHEDULED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + id));
        assertEquals(date, TestDatabase.queryString("SELECT appointment_date FROM appointments WHERE id = " + id));
    }

    @Test
    public void createAppointmentInThePastReturns400() throws Exception {
        int donorId = seedDonor();
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().createAppointment(
                new CreateAppointmentRequest(donorId, LocalDate.now().minusDays(1).toString(), "09:30", "PLASMA"))));
    }

    @Test
    public void updateAppointmentChangesFieldsAndClearsRescheduleRequest() throws Exception {
        int donorId = seedDonor();
        String date = futureDate(2);
        int id = TestDatabase.insertAppointment(donorId, date, "10:00:00");
        // Simulate a donor reschedule request flag that update must clear (D18).
        TestDatabase.execute("UPDATE appointments SET reschedule_requested_at = NOW() WHERE id = " + id);

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().updateAppointment(
                new UpdateAppointmentRequest(id, donorId, date, "11:15", "BLOOD")));
        assertTrue(response.isSuccess());

        assertEquals("11:15:00", TestDatabase.queryString("SELECT appointment_time FROM appointments WHERE id = " + id));
        assertEquals("BLOOD", TestDatabase.queryString("SELECT target_procedure FROM appointments WHERE id = " + id));
        assertNull(TestDatabase.queryString("SELECT reschedule_requested_at FROM appointments WHERE id = " + id));
    }

    @Test
    public void cancelAppointmentRecordsAuditAndRemovesFromActive() throws Exception {
        int donorId = seedDonor();
        String date = futureDate(4);
        int id = TestDatabase.insertAppointment(donorId, date, "10:00:00");

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().cancelAppointment(
                new CancelAppointmentRequest(id, donorId, "El donante avisó", 1)));
        assertTrue(response.isSuccess());

        assertEquals("CANCELLED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + id));
        assertEquals(1, TestDatabase.queryInt("SELECT cancelled_by_user_id FROM appointments WHERE id = " + id));
        assertEquals("El donante avisó", TestDatabase.queryString("SELECT cancel_reason FROM appointments WHERE id = " + id));
        assertNotNull(TestDatabase.queryString("SELECT cancelled_at FROM appointments WHERE id = " + id));

        List<Appointment> active = ApiTestClient.call(ApiTestClient.api().getAppointments(date)).getData();
        assertTrue(active.stream().noneMatch(a -> a.getId() == id));
    }

    @Test
    public void markAttendanceMovesTurnToAttendedOrNoShow() throws Exception {
        int donorId = seedDonor();
        int attendedId = TestDatabase.insertAppointment(donorId, LocalDate.now().toString(), "08:00:00");
        int noShowId = TestDatabase.insertAppointment(donorId, LocalDate.now().toString(), "08:30:00");

        assertTrue(ApiTestClient.call(ApiTestClient.api().markAttendance(
                new MarkAttendanceRequest(attendedId, donorId, "ATTENDED", 1))).isSuccess());
        assertTrue(ApiTestClient.call(ApiTestClient.api().markAttendance(
                new MarkAttendanceRequest(noShowId, donorId, "NO_SHOW", 1))).isSuccess());

        assertEquals("ATTENDED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + attendedId));
        assertEquals("NO_SHOW", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + noShowId));
    }

    @Test
    public void markAttendanceOnFutureTurnReturns409() throws Exception {
        int donorId = seedDonor();
        int id = TestDatabase.insertAppointment(donorId, futureDate(5), "08:00:00");
        assertEquals(409, ApiTestClient.status(ApiTestClient.api().markAttendance(
                new MarkAttendanceRequest(id, donorId, "ATTENDED", 1))));
    }

    @Test
    public void rescheduleCancelsOldAndLinksNewTurn() throws Exception {
        int donorId = seedDonor();
        String oldDate = futureDate(2);
        String newDate = futureDate(6);
        int oldId = TestDatabase.insertAppointment(donorId, oldDate, "10:00:00");

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().rescheduleAppointment(
                new RescheduleAppointmentRequest(oldId, donorId, newDate, "12:00", "PLATELETS", 1)));
        assertTrue(response.isSuccess());

        assertEquals("CANCELLED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + oldId));
        assertEquals("RESCHEDULED", TestDatabase.queryString("SELECT cancel_reason FROM appointments WHERE id = " + oldId));

        int newId = TestDatabase.queryInt("SELECT id FROM appointments WHERE donor_id = " + donorId
                + " AND appointment_date = '" + newDate + "'");
        assertTrue(newId > 0);
        assertEquals(newId, TestDatabase.queryInt(
                "SELECT rescheduled_to_appointment_id FROM appointments WHERE id = " + oldId));
        assertEquals("SCHEDULED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + newId));
    }

    @Test
    public void rescheduleIntoSameDayExistingTurnReturns409() throws Exception {
        int donorId = seedDonor();
        int oldId = TestDatabase.insertAppointment(donorId, futureDate(2), "10:00:00");
        // Another active turn already exists on the target day.
        TestDatabase.insertAppointment(donorId, futureDate(7), "09:00:00");

        assertEquals(409, ApiTestClient.status(ApiTestClient.api().rescheduleAppointment(
                new RescheduleAppointmentRequest(oldId, donorId, futureDate(7), "12:00", "PLASMA", 1))));
    }

    @Test
    public void registerDonationLinksAppointmentAndMarksAttended() throws Exception {
        int donorId = seedDonor();
        int appointmentId = TestDatabase.insertAppointment(donorId, LocalDate.now().toString(), "08:00:00");

        Donation donation = new Donation(donorId, 70.0, 1.75, 42.0, 180000, 1, Collections.singletonList("PLASMA"));
        donation.setAppointmentId(appointmentId);

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().saveDonation(donation));
        assertTrue(response.isSuccess());

        assertEquals("ATTENDED", TestDatabase.queryString("SELECT status FROM appointments WHERE id = " + appointmentId));
        assertEquals(appointmentId, TestDatabase.queryInt(
                "SELECT appointment_id FROM donations WHERE donor_id = " + donorId));
    }

    @Test
    public void statusFilterSplitsActiveFromHistory() throws Exception {
        int donorId = seedDonor();
        int activeId = TestDatabase.insertAppointment(donorId, futureDate(3), "10:00:00");
        int cancelledId = TestDatabase.insertAppointment(donorId, futureDate(3), "11:00:00");
        TestDatabase.execute("UPDATE appointments SET status = 'CANCELLED' WHERE id = " + cancelledId);

        List<Appointment> active = ApiTestClient.call(
                ApiTestClient.api().getAppointmentsFiltered(futureDate(3), "SCHEDULED")).getData();
        assertTrue(active.stream().anyMatch(a -> a.getId() == activeId));
        assertTrue(active.stream().noneMatch(a -> a.getId() == cancelledId));

        List<Appointment> all = ApiTestClient.call(
                ApiTestClient.api().getAppointmentsFiltered(futureDate(3), "all")).getData();
        assertTrue(all.stream().anyMatch(a -> a.getId() == activeId));
        assertTrue(all.stream().anyMatch(a -> a.getId() == cancelledId));
    }
}
