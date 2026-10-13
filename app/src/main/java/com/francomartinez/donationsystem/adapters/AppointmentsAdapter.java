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
                            && Objects.equals(oldItem.getAppointmentTime(), newItem.getAppointmentTime());
                }
            };

    private final OnReminderClickListener reminderListener;
    private OnDonorClickListener donorClickListener;

    public AppointmentsAdapter(OnReminderClickListener reminderListener) {
        super(DIFF_CALLBACK);
        this.reminderListener = reminderListener;
    }

    public void setOnDonorClickListener(OnDonorClickListener listener) {
        this.donorClickListener = listener;
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
        final TextView textViewAppointmentDonor;
        final TextView textViewAppointmentProcedure;
        final MaterialButton buttonSendWhatsApp;
        final MaterialButton buttonSendEmail;
        final ImageView imageViewReminderWhatsAppChecked;
        final ImageView imageViewReminderEmailChecked;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            chipAppointmentTime = itemView.findViewById(R.id.chipAppointmentTime);
            textViewAppointmentDonor = itemView.findViewById(R.id.textViewAppointmentDonor);
            textViewAppointmentProcedure = itemView.findViewById(R.id.textViewAppointmentProcedure);
            buttonSendWhatsApp = itemView.findViewById(R.id.buttonSendWhatsApp);
            buttonSendEmail = itemView.findViewById(R.id.buttonSendEmail);
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

            boolean sameDay = LocalDate.now().toString().equals(appointment.getAppointmentDate());

            bindChannel(appointment.isReminderSent(), sameDay,
                    buttonSendWhatsApp, imageViewReminderWhatsAppChecked, R.string.cd_send_whatsapp, "whatsapp", appointment);
            bindChannel(appointment.isEmailReminderSent(), sameDay,
                    buttonSendEmail, imageViewReminderEmailChecked, R.string.cd_send_email, "email", appointment);
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