package com.francomartinez.donationsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.francomartinez.donationsystem.R;
import com.francomartinez.donationsystem.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.time.LocalDate;
import java.util.Objects;

public class AppointmentsAdapter extends ListAdapter<Appointment, AppointmentsAdapter.AppointmentViewHolder> {

    public interface OnReminderClickListener {
        void onReminderClick(Appointment appointment, String channel);
    }

    public interface OnDonorClickListener {
        void onDonorClick(Appointment appointment);
    }

    public interface OnActionsClickListener {
        void onActionsClick(Appointment appointment, View anchor);
    }

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Appointment>() {
                @Override
                public boolean areItemsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    return oldItem.isReminderSent() == newItem.isReminderSent()
                            && oldItem.isEmailReminderSent() == newItem.isEmailReminderSent()
                            && Objects.equals(oldItem.getAppointmentTime(), newItem.getAppointmentTime())
                            && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && Objects.equals(oldItem.getAppointmentDate(), newItem.getAppointmentDate())
                            && Objects.equals(oldItem.getTargetProcedure(), newItem.getTargetProcedure());
                }
            };

    private final OnReminderClickListener reminderListener;
    private OnDonorClickListener donorClickListener;
    private OnActionsClickListener actionsClickListener;
    private boolean showStatusChip = false;
    private boolean requestsMode = false;

    public AppointmentsAdapter(OnReminderClickListener reminderListener) {
        super(DIFF_CALLBACK);
        this.reminderListener = reminderListener;
    }

    public void setShowStatusChip(boolean showStatusChip) {
        this.showStatusChip = showStatusChip;
    }

    public void setRequestsMode(boolean requestsMode) {
        this.requestsMode = requestsMode;
    }

    public void setOnDonorClickListener(OnDonorClickListener listener) {
        this.donorClickListener = listener;
    }

    public void setOnActionsClickListener(OnActionsClickListener listener) {
        this.actionsClickListener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class AppointmentViewHolder extends RecyclerView.ViewHolder {
        final Chip chipAppointmentTime;
        final Chip chipAppointmentStatus;
        final TextView textViewAppointmentDonor;
        final TextView textViewAppointmentProcedure;
        final MaterialButton buttonSendWhatsApp;
        final MaterialButton buttonSendEmail;
        final MaterialButton buttonAppointmentActions;
        final ImageView imageViewReminderWhatsAppChecked;
        final ImageView imageViewReminderEmailChecked;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            chipAppointmentTime = itemView.findViewById(R.id.chipAppointmentTime);
            chipAppointmentStatus = itemView.findViewById(R.id.chipAppointmentStatus);
            textViewAppointmentDonor = itemView.findViewById(R.id.textViewAppointmentDonor);
            textViewAppointmentProcedure = itemView.findViewById(R.id.textViewAppointmentProcedure);
            buttonSendWhatsApp = itemView.findViewById(R.id.buttonSendWhatsApp);
            buttonSendEmail = itemView.findViewById(R.id.buttonSendEmail);
            buttonAppointmentActions = itemView.findViewById(R.id.buttonAppointmentActions);
            imageViewReminderWhatsAppChecked = itemView.findViewById(R.id.imageViewReminderWhatsAppChecked);
            imageViewReminderEmailChecked = itemView.findViewById(R.id.imageViewReminderEmailChecked);
        }

        void bind(Appointment appointment) {
            itemView.setOnClickListener(v -> {
                if (donorClickListener != null) {
                    donorClickListener.onDonorClick(appointment);
                }
            });

            chipAppointmentTime.setText(appointment.getAppointmentTime());
            textViewAppointmentDonor.setText(appointment.getDonorName());
            textViewAppointmentProcedure.setText(procedureLabel(appointment.getTargetProcedure()));

            bindStatusChip(appointment);

            boolean scheduled = appointment.getStatus() == null || "SCHEDULED".equals(appointment.getStatus());
            buttonAppointmentActions.setVisibility(scheduled ? View.VISIBLE : View.GONE);
            buttonAppointmentActions.setOnClickListener(v -> {
                if (actionsClickListener != null) {
                    actionsClickListener.onActionsClick(appointment, buttonAppointmentActions);
                }
            });

            // Recordatorios sólo para turnos activos (SCHEDULED).
            if (!scheduled) {
                buttonSendWhatsApp.setVisibility(View.GONE);
                buttonSendEmail.setVisibility(View.GONE);
                imageViewReminderWhatsAppChecked.setVisibility(View.GONE);
                imageViewReminderEmailChecked.setVisibility(View.GONE);
                return;
            }

            boolean sameDay = LocalDate.now().toString().equals(appointment.getAppointmentDate());

            bindChannel(appointment.isReminderSent(), sameDay,
                    buttonSendWhatsApp, imageViewReminderWhatsAppChecked, R.string.cd_send_whatsapp, "whatsapp", appointment);
            bindChannel(appointment.isEmailReminderSent(), sameDay,
                    buttonSendEmail, imageViewReminderEmailChecked, R.string.cd_send_email, "email", appointment);
        }

        private void bindStatusChip(Appointment appointment) {
            String status = appointment.getStatus();
            boolean active = status == null || "SCHEDULED".equals(status);

            // Modo "Solicitudes": turno activo con pedido de reagendado del donante (D18).
            if (requestsMode) {
                if (active && hasRescheduleRequest(appointment)) {
                    chipAppointmentStatus.setText(R.string.chip_status_reschedule_requested);
                    chipAppointmentStatus.setChipBackgroundColorResource(R.color.state_recovery);
                    chipAppointmentStatus.setVisibility(View.VISIBLE);
                } else {
                    chipAppointmentStatus.setVisibility(View.GONE);
                }
                return;
            }

            if (!showStatusChip || active) {
                chipAppointmentStatus.setVisibility(View.GONE);
                return;
            }

            boolean rescheduled = "CANCELLED".equals(status)
                    && appointment.getRescheduledToAppointmentId() != null
                    && appointment.getRescheduledToAppointmentId() > 0;

            int labelRes;
            int colorRes;
            if (rescheduled) {
                labelRes = R.string.chip_status_rescheduled;
                colorRes = R.color.medical_blue;
            } else if ("CANCELLED".equals(status)) {
                labelRes = R.string.chip_status_cancelled;
                colorRes = R.color.veda_red;
            } else if ("ATTENDED".equals(status)) {
                labelRes = R.string.chip_status_attended;
                colorRes = R.color.aptitude_green;
            } else if ("NO_SHOW".equals(status)) {
                labelRes = R.string.chip_status_no_show;
                colorRes = R.color.status_no_show;
            } else {
                chipAppointmentStatus.setVisibility(View.GONE);
                return;
            }

            chipAppointmentStatus.setText(labelRes);
            chipAppointmentStatus.setChipBackgroundColorResource(colorRes);
            chipAppointmentStatus.setVisibility(View.VISIBLE);
        }

        private boolean hasRescheduleRequest(Appointment appointment) {
            String requestedAt = appointment.getRescheduleRequestedAt();
            return requestedAt != null && !requestedAt.trim().isEmpty()
                    && !"null".equalsIgnoreCase(requestedAt.trim());
        }

        private void bindChannel(boolean sent, boolean sameDay, MaterialButton button, ImageView check,
                                 int contentDescriptionRes, String channel, Appointment appointment) {
            if (sent) {
                check.setVisibility(View.VISIBLE);
                button.setVisibility(View.GONE);
                return;
            }
            check.setVisibility(View.GONE);
            button.setVisibility(View.VISIBLE);
            button.setContentDescription(itemView.getContext().getString(contentDescriptionRes));
            button.setEnabled(!sameDay);
            button.setAlpha(sameDay ? 0.4f : 1f);
            button.setOnClickListener(v -> {
                if (reminderListener != null) {
                    reminderListener.onReminderClick(appointment, channel);
                }
            });
        }

        private String procedureLabel(String code) {
            if (code == null) return "";
            switch (code.toUpperCase()) {
                case "PLASMA":
                    return itemView.getContext().getString(R.string.procedure_plasma);
                case "PLATELETS":
                    return itemView.getContext().getString(R.string.procedure_platelets);
                case "BLOOD":
                    return itemView.getContext().getString(R.string.procedure_blood);
                default:
                    return code;
            }
        }
    }
}