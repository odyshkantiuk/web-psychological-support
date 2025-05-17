document.addEventListener('DOMContentLoaded', function() {
    const availabilityForm = document.getElementById('availabilityForm');
    const saveAvailabilityBtn = document.getElementById('saveAvailability');

    if (saveAvailabilityBtn) {
        saveAvailabilityBtn.addEventListener('click', function() {
            saveAvailabilitySettings();
        });
    }

    const weekdayAvailableCheckboxes = document.querySelectorAll('.weekday-available');
    if (weekdayAvailableCheckboxes.length > 0) {
        weekdayAvailableCheckboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                const accordionBody = this.closest('.accordion-body');
                if (accordionBody) {
                    const timeSlots = accordionBody.querySelector('.time-slots');
                    if (timeSlots) {
                        timeSlots.style.display = this.checked ? 'flex' : 'none';
                    }
                }
            });

            const isChecked = checkbox.checked;
            const accordionBody = checkbox.closest('.accordion-body');
            if (accordionBody) {
                const timeSlots = accordionBody.querySelector('.time-slots');
                if (timeSlots) {
                    timeSlots.style.display = isChecked ? 'flex' : 'none';
                }
            }
        });
    }

    const addTimeSlotButtons = document.querySelectorAll('.add-time-slot');
    if (addTimeSlotButtons.length > 0) {
        addTimeSlotButtons.forEach(button => {
            button.addEventListener('click', function() {
                const timeSlots = this.closest('.time-slots').parentElement;
                const newTimeSlot = createTimeSlotRow();
                timeSlots.appendChild(newTimeSlot);
            });
        });
    }


    function createTimeSlotRow() {
        const row = document.createElement('div');
        row.className = 'row time-slot-row mt-2';

        row.innerHTML = `
            <div class="col-md-5">
                <input type="time" class="form-control time-start" value="09:00">
            </div>
            <div class="col-md-5">
                <input type="time" class="form-control time-end" value="17:00">
            </div>
            <div class="col-md-2 d-flex align-items-end">
                <button type="button" class="btn btn-outline-danger remove-time-slot w-100">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        `;

        const removeBtn = row.querySelector('.remove-time-slot');
        removeBtn.addEventListener('click', function() {
            row.remove();
        });

        return row;
    }

    function saveAvailabilitySettings() {
        const availabilityData = collectAvailabilityData();

        fetch('/api/availability', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
            },
            body: JSON.stringify(availabilityData)
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to save availability settings');
                }
                return response.json();
            })
            .then(data => {
                showAvailabilitySuccessMessage();

                const modal = bootstrap.Modal.getInstance(document.getElementById('setAvailabilityModal'));
                if (modal) {
                    modal.hide();
                }
            })
            .catch(error => {
                console.error('Error saving availability:', error);
                alert('Failed to save availability settings. Please try again.');
            });
    }

    function collectAvailabilityData() {
        const data = {
            weeklySchedule: [],
            appointmentSettings: {
                duration: 60,
                bufferTime: 15
            },
            exceptions: []
        };

        const daysOfWeek = ['monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday', 'sunday'];

        daysOfWeek.forEach((day, index) => {
            const availableCheckbox = document.getElementById(`${day}Available`);

            if (availableCheckbox && availableCheckbox.checked) {
                const timeSlotRows = document.querySelectorAll(`#${day}Collapse .time-slot-row`);
                const mainTimeSlot = {
                    startTime: document.getElementById(`${day}Start`)?.value || '09:00',
                    endTime: document.getElementById(`${day}End`)?.value || '17:00'
                };

                const timeSlots = [mainTimeSlot];

                timeSlotRows.forEach(row => {
                    const startInput = row.querySelector('.time-start');
                    const endInput = row.querySelector('.time-end');

                    if (startInput && endInput) {
                        timeSlots.push({
                            startTime: startInput.value,
                            endTime: endInput.value
                        });
                    }
                });

                data.weeklySchedule.push({
                    dayOfWeek: index + 1,
                    available: true,
                    timeSlots: timeSlots
                });
            } else {
                data.weeklySchedule.push({
                    dayOfWeek: index + 1,
                    available: false,
                    timeSlots: []
                });
            }
        });


        return data;
    }

    function showAvailabilitySuccessMessage() {
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-success alert-dismissible fade show';
        alertDiv.setAttribute('role', 'alert');
        alertDiv.innerHTML = `
            <strong>Success!</strong> Your availability has been updated.
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;

        const section = document.querySelector('section');
        if (section) {
            section.insertBefore(alertDiv, section.firstChild);

            setTimeout(() => {
                const bootstrapAlert = new bootstrap.Alert(alertDiv);
                bootstrapAlert.close();
            }, 5000);
        }
    }
});