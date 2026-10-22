package com.francomartinez.donationsystem.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.francomartinez.donationsystem.models.ApiResponse;
import com.francomartinez.donationsystem.models.Appointment;
import com.francomartinez.donationsystem.models.requests.CancelAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.CreateAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.MarkAttendanceRequest;
import com.francomartinez.donationsystem.models.requests.ReminderRequest;
import com.francomartinez.donationsystem.models.requests.RescheduleAppointmentRequest;
import com.francomartinez.donationsystem.models.requests.UpdateAppointmentRequest;
import com.francomartinez.donationsystem.repositories.AppointmentRepository;
import com.francomartinez.donationsystem.utils.Resource;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentsViewModel extends ViewModel {

    private final AppointmentRepository appointmentRepository;
    private final MutableLiveData<Resource<List<Appointment>>> appointmentsResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<Appointment>>> monthResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> reminderResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> createResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> dispatchResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> updateResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> rescheduleResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> cancelResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Map<String, Object>>> attendanceResult = new MutableLiveData<>();

    public AppointmentsViewModel() {
        this.appointmentRepository = new AppointmentRepository();
    }

    public LiveData<Resource<List<Appointment>>> getAppointmentsResult() {
        return appointmentsResult;
    }

    public LiveData<Resource<List<Appointment>>> getMonthResult() {
        return monthResult;
    }

    public LiveData<Resource<Map<String, Object>>> getReminderResult() {
        return reminderResult;
    }

    public LiveData<Resource<Map<String, Object>>> getCreateResult() {
        return createResult;
    }

    public LiveData<Resource<Map<String, Object>>> getDispatchResult() {
        return dispatchResult;
    }

    public LiveData<Resource<Map<String, Object>>> getUpdateResult() {
        return updateResult;
    }

    public LiveData<Resource<Map<String, Object>>> getRescheduleResult() {
        return rescheduleResult;
    }

    public LiveData<Resource<Map<String, Object>>> getCancelResult() {
        return cancelResult;
    }

    public LiveData<Resource<Map<String, Object>>> getAttendanceResult() {
        return attendanceResult;
    }

    public void updateAppointment(UpdateAppointmentRequest request) {
        updateResult.postValue(Resource.loading());
        appointmentRepository.updateAppointment(request, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                handleActionResponse(updateResult, response, "No se pudo actualizar el turno");
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                updateResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void rescheduleAppointment(RescheduleAppointmentRequest request) {
        rescheduleResult.postValue(Resource.loading());
        appointmentRepository.rescheduleAppointment(request, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                handleActionResponse(rescheduleResult, response, "No se pudo reagendar el turno");
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                rescheduleResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void cancelAppointment(CancelAppointmentRequest request) {
        cancelResult.postValue(Resource.loading());
        appointmentRepository.cancelAppointment(request, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                handleActionResponse(cancelResult, response, "No se pudo cancelar el turno");
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                cancelResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void markAttendance(MarkAttendanceRequest request) {
        attendanceResult.postValue(Resource.loading());
        appointmentRepository.markAttendance(request, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                handleActionResponse(attendanceResult, response, "No se pudo registrar la asistencia");
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                attendanceResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    private void handleActionResponse(MutableLiveData<Resource<Map<String, Object>>> target,
                                      Response<ApiResponse<Map<String, Object>>> response,
                                      String fallbackError) {
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse<Map<String, Object>> body = response.body();
            if (body.isSuccess()) {
                target.postValue(Resource.success(body.getData()));
            } else {
                target.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : fallbackError));
            }
        } else {
            target.postValue(Resource.error(fallbackError + ": " + response.code()));
        }
    }

    public void loadAppointments(String weekDate, String status) {
        appointmentsResult.postValue(Resource.loading());
        appointmentRepository.getAppointmentsFiltered(weekDate, status, new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Appointment>> body = response.body();
                    if (body.isSuccess()) {
                        appointmentsResult.postValue(Resource.success(body.getData()));
                    } else {
                        appointmentsResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo recuperar la agenda"));
                    }
                } else {
                    appointmentsResult.postValue(Resource.error("No se pudo recuperar la agenda: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                appointmentsResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void loadMonthAppointments(String month, String status) {
        monthResult.postValue(Resource.loading());
        appointmentRepository.getAppointmentsByMonthFiltered(month, status, new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Appointment>> body = response.body();
                    if (body.isSuccess()) {
                        monthResult.postValue(Resource.success(body.getData()));
                    } else {
                        monthResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo recuperar la agenda"));
                    }
                } else {
                    monthResult.postValue(Resource.error("No se pudo recuperar la agenda: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                monthResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void dispatchReminders() {
        dispatchResult.postValue(Resource.loading());
        appointmentRepository.dispatchReminders(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        dispatchResult.postValue(Resource.success(body.getData()));
                    } else {
                        dispatchResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo lanzar el envío"));
                    }
                } else {
                    dispatchResult.postValue(Resource.error("No se pudo lanzar el envío: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                dispatchResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void loadAppointments(String weekDate) {
        appointmentsResult.postValue(Resource.loading());
        appointmentRepository.getAppointments(weekDate, new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Appointment>> body = response.body();
                    if (body.isSuccess()) {
                        appointmentsResult.postValue(Resource.success(body.getData()));
                    } else {
                        appointmentsResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo recuperar la agenda"));
                    }
                } else {
                    appointmentsResult.postValue(Resource.error("No se pudo recuperar la agenda: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                appointmentsResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void sendReminder(int appointmentId) {
        sendReminder(appointmentId, "whatsapp");
    }

    public void sendReminder(int appointmentId, String channel) {
        reminderResult.postValue(Resource.loading());
        appointmentRepository.sendReminder(
                new ReminderRequest(appointmentId, getCurrentUserReminderId(), channel),
                new Callback<ApiResponse<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponse<Map<String, Object>> body = response.body();
                            if (body.isSuccess()) {
                                reminderResult.postValue(Resource.success(body.getData()));
                            } else {
                                reminderResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo actualizar el recordatorio"));
                            }
                        } else {
                            reminderResult.postValue(Resource.error("No se pudo actualizar el recordatorio: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                        reminderResult.postValue(Resource.error("Error de red: " + t.getMessage()));
                    }
                });
    }

    public void loadMonthAppointments(String month) {
        monthResult.postValue(Resource.loading());
        appointmentRepository.getAppointmentsByMonth(month, new Callback<ApiResponse<List<Appointment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Appointment>>> call, Response<ApiResponse<List<Appointment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<Appointment>> body = response.body();
                    if (body.isSuccess()) {
                        monthResult.postValue(Resource.success(body.getData()));
                    } else {
                        monthResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo recuperar la agenda"));
                    }
                } else {
                    monthResult.postValue(Resource.error("No se pudo recuperar la agenda: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Appointment>>> call, Throwable t) {
                monthResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    public void createAppointment(CreateAppointmentRequest request) {
        createResult.postValue(Resource.loading());
        appointmentRepository.createAppointment(request, new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<ApiResponse<Map<String, Object>>> call, Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Map<String, Object>> body = response.body();
                    if (body.isSuccess()) {
                        createResult.postValue(Resource.success(body.getData()));
                    } else {
                        createResult.postValue(Resource.error(body.getMessage() != null ? body.getMessage() : "No se pudo agendar el turno"));
                    }
                } else {
                    createResult.postValue(Resource.error("No se pudo agendar el turno: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Map<String, Object>>> call, Throwable t) {
                createResult.postValue(Resource.error("Error de red: " + t.getMessage()));
            }
        });
    }

    private Integer currentUserId;

    public void setCurrentUserId(Integer userId) {
        this.currentUserId = userId;
    }

    public Integer getCurrentUserId() {
        return currentUserId;
    }

    private Integer getCurrentUserReminderId() {
        return currentUserId;
    }
}