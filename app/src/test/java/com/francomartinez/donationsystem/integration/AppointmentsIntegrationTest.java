package com.francomartinez.donationsystem.integration;

import com.francomartinez.donationsystem.integration.support.ApiTestClient;
import com.francomartinez.donationsystem.integration.support.BaseApiTest;
import com.francomartinez.donationsystem.integration.support.TestDatabase;
import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AppointmentsIntegrationTest extends BaseApiTest {

    @Test
    public void listReturnsSeededAppointmentsWithDonorAndPhone() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", null);
        TestDatabase.insertDonorPhone(donorId, "3514444444", true);
        String today = LocalDate.now().toString();
        TestDatabase.insertAppointment(donorId, today, "10:30:00");
        TestDatabase.insertAppointment(donorId, today, "11:00:00");

        ApiResponse<List<Appointment>> response = ApiTestClient.call(ApiTestClient.api().getAppointments(today));
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        Appointment first = response.getData().get(0);
        assertEquals("Maria Lopez", first.getDonorName());
        assertEquals("3514444444", first.getDonorPhone());
        assertEquals("PLASMA", first.getTargetProcedure());
        assertEquals("SCHEDULED", first.getStatus());
        assertEquals(false, first.isReminderSent());
    }

    @Test
    public void sendReminderPersistsFlagsAndUserId() throws Exception {
        int donorId = TestDatabase.insertDonor("33333333", "Maria", "Lopez", "O+", null);
        int appointmentId = TestDatabase.insertAppointment(donorId, LocalDate.now().toString(), "10:30:00");

        ApiResponse<Map<String, Object>> response = ApiTestClient.call(ApiTestClient.api().sendWhatsAppReminder(
                new ReminderRequest(appointmentId, 1)));
        assertTrue(response.isSuccess());

        assertEquals(1, TestDatabase.queryInt("SELECT is_reminder_sent FROM appointments WHERE id = " + appointmentId));
        assertEquals(1, TestDatabase.queryInt("SELECT reminder_sent_by_user_id FROM appointments WHERE id = " + appointmentId));
        assertNotNull(TestDatabase.queryString("SELECT reminder_sent_at FROM appointments WHERE id = " + appointmentId));

        List<Appointment> refreshed = ApiTestClient.call(ApiTestClient.api().getAppointments(LocalDate.now().toString())).getData();
        assertTrue(refreshed.stream().anyMatch(a -> a.getId() == appointmentId && a.isReminderSent()));
    }

    @Test
    public void sendReminderWithInvalidIdReturns400() throws Exception {
        assertEquals(400, ApiTestClient.status(ApiTestClient.api().sendWhatsAppReminder(new ReminderRequest(0, 1))));
    }
}