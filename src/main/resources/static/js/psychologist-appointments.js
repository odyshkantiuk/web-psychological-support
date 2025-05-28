document.addEventListener('DOMContentLoaded', function() {
    initializeAppointmentFilters();

    initializeModalForms();

    initializeCalendarView();

    const viewTabs = document.querySelectorAll('[data-bs-toggle="tab"]');
    if (viewTabs.length > 0) {
        viewTabs.forEach(tab => {
            tab.addEventListener('shown.bs.tab', function(e) {
                if (e.target.id === 'calendar-tab') {
                    refreshCalendarView();
                }
            });
        });
    }
});

function initializeAppointmentFilters() {
    const filterStatus = document.getElementById('filterStatus');
    const filterDateFrom = document.getElementById('filterDateFrom');
    const filterDateTo = document.getElementById('filterDateTo');
    const applyFilters = document.getElementById('applyFilters');
    const clearFilters = document.getElementById('clearFilters');
    const tableRows = document.querySelectorAll('#appointmentsTable tbody tr');

    if (applyFilters) {
        applyFilters.addEventListener('click', function() {
            filterAppointments();
        });
    }

    if (clearFilters) {
        clearFilters.addEventListener('click', function() {
            if (filterStatus) filterStatus.value = '';
            if (filterDateFrom) filterDateFrom.value = '';
            if (filterDateTo) filterDateTo.value = '';
            filterAppointments();
        });
    }

    function filterAppointments() {
        if (!tableRows) return;

        const status = filterStatus ? filterStatus.value : '';
        const dateFrom = filterDateFrom ? filterDateFrom.value : '';
        const dateTo = filterDateTo ? filterDateTo.value : '';

        tableRows.forEach(row => {
            let showRow = true;

            if (status && row.getAttribute('data-status') !== status) {
                showRow = false;
            }

            const rowDate = row.getAttribute('data-date');
            if (showRow && dateFrom && rowDate < dateFrom) {
                showRow = false;
            }
            if (showRow && dateTo && rowDate > dateTo) {
                showRow = false;
            }

            row.style.display = showRow ? '' : 'none';
        });
    }
}

function initializeModalForms() {
    const completeAppointmentModal = document.getElementById('completeAppointmentModal');
    if (completeAppointmentModal) {
        completeAppointmentModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const appointmentId = button.getAttribute('data-appointment-id');
            const row = button.closest('tr');
            const clientId = row.querySelector('a[href*="clientId"]')?.href.split('clientId=')[1] || '';
            
            document.getElementById('completeAppointmentId').value = appointmentId;

            const form = document.getElementById('completeAppointmentForm');
            form.action = `/api/appointments/${appointmentId}/status`;

            let clientIdInput = document.getElementById('completeClientId');
            if (!clientIdInput) {
                clientIdInput = document.createElement('input');
                clientIdInput.type = 'hidden';
                clientIdInput.id = 'completeClientId';
                clientIdInput.name = 'clientId';
                form.appendChild(clientIdInput);
            }
            clientIdInput.value = clientId;
        });
    }

    const rescheduleAppointmentModal = document.getElementById('rescheduleAppointmentModal');
    if (rescheduleAppointmentModal) {
        rescheduleAppointmentModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const appointmentId = button.getAttribute('data-appointment-id');
            const row = button.closest('tr');
            const clientId = row.querySelector('a[href*="clientId"]')?.href.split('clientId=')[1] || '';
            
            document.getElementById('rescheduleAppointmentId').value = appointmentId;
            document.getElementById('rescheduleClientId').value = clientId;

            const today = new Date().toISOString().split('T')[0];
            document.getElementById('rescheduleAppointmentDate').min = today;
            document.getElementById('rescheduleAppointmentDate').value = today;

            const availableTimesContainer = document.getElementById('rescheduleAvailableTimesContainer');
            availableTimesContainer.innerHTML = `
                <div class="alert alert-info">
                    <div class="d-flex align-items-center">
                        <div class="spinner-border spinner-border-sm me-2" role="status">
                            <span class="visually-hidden">Loading...</span>
                        </div>
                        <div>Loading available times...</div>
                    </div>
                </div>
            `;

            fetchAvailableTimes(clientId);

            document.getElementById('rescheduleAppointmentDate').addEventListener('change', function() {
                fetchAvailableTimes(clientId);
            });
        });
    }

    const cancelAppointmentModal = document.getElementById('cancelAppointmentModal');
    if (cancelAppointmentModal) {
        cancelAppointmentModal.addEventListener('show.bs.modal', function(event) {
            const button = event.relatedTarget;
            const appointmentId = button.getAttribute('data-appointment-id');
            const row = button.closest('tr');
            const clientId = row.querySelector('a[href*="clientId"]')?.href.split('clientId=')[1] || '';
            
            document.getElementById('cancelAppointmentId').value = appointmentId;

            const form = document.getElementById('cancelAppointmentForm');
            form.action = `/api/appointments/${appointmentId}/status`;

            let clientIdInput = document.getElementById('cancelClientId');
            if (!clientIdInput) {
                clientIdInput = document.createElement('input');
                clientIdInput.type = 'hidden';
                clientIdInput.id = 'cancelClientId';
                clientIdInput.name = 'clientId';
                form.appendChild(clientIdInput);
            }
            clientIdInput.value = clientId;
        });
    }

    const rescheduleAppointmentForm = document.getElementById('rescheduleAppointmentForm');
    if (rescheduleAppointmentForm) {
        rescheduleAppointmentForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const appointmentId = document.getElementById('rescheduleAppointmentId').value;
            const startTime = document.getElementById('rescheduleStartTime').value;
            const endTime = document.getElementById('rescheduleEndTime').value;
            const reason = document.getElementById('rescheduleReason').value;

            if (!appointmentId || !startTime || !endTime || !reason) {
                alert('Please select a time slot and provide a reason for rescheduling');
                return;
            }

            const formData = {
                id: appointmentId,
                startTime: startTime,
                endTime: endTime,
                notes: reason + '\n\n[Rescheduled]'
            };

            fetch(`/api/appointments/${appointmentId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
                },
                body: JSON.stringify(formData)
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Failed to reschedule appointment');
                    }
                    return response.json();
                })
                .then(data => {
                    showNotification('Appointment rescheduled successfully', 'success');

                    const modal = bootstrap.Modal.getInstance(rescheduleAppointmentModal);
                    modal.hide();

                    setTimeout(() => {
                        window.location.reload();
                    }, 1000);
                })
                .catch(error => {
                    console.error('Error rescheduling appointment:', error);
                    showNotification('Failed to reschedule appointment. Please try again.', 'error');
                });
        });
    }

    const cancelAppointmentForm = document.getElementById('cancelAppointmentForm');
    if (cancelAppointmentForm) {
        cancelAppointmentForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const appointmentId = document.getElementById('cancelAppointmentId').value;
            const psychologistId = document.getElementById('cancelPsychologistId').value;
            const cancelReason = document.getElementById('cancelReason').value;
            const clientId = document.getElementById('cancelClientId')?.value || '';

            if (!appointmentId || !cancelReason) {
                alert('Please fill out all required fields');
                return;
            }

            let url = `/api/appointments/${appointmentId}/status?status=CANCELLED&psychologistId=${psychologistId}`;
            if (clientId) {
                url += `&clientId=${clientId}`;
            }

            fetch(url, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
                },
                body: JSON.stringify({
                    notes: cancelReason
                })
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to cancel appointment');
                }
                return response.json();
            })
            .then(data => {
                showNotification('Appointment cancelled successfully', 'success');

                const modal = bootstrap.Modal.getInstance(cancelAppointmentModal);
                modal.hide();

                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            })
            .catch(error => {
                console.error('Error cancelling appointment:', error);
                showNotification('Failed to cancel appointment. Please try again.', 'error');
            });
        });
    }

    const completeAppointmentForm = document.getElementById('completeAppointmentForm');
    if (completeAppointmentForm) {
        completeAppointmentForm.addEventListener('submit', function(e) {
            e.preventDefault();

            const appointmentId = document.getElementById('completeAppointmentId').value;
            const psychologistId = document.getElementById('completePsychologistId').value;
            const sessionNotes = document.getElementById('sessionNotes').value || '';
            const clientId = document.getElementById('completeClientId')?.value || '';

            if (!appointmentId) {
                alert('Missing appointment ID');
                return;
            }

            let url = `/api/appointments/${appointmentId}/status?status=COMPLETED&psychologistId=${psychologistId}`;
            if (clientId) {
                url += `&clientId=${clientId}`;
            }

            fetch(url, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ''
                },
                body: sessionNotes ? JSON.stringify({
                    notes: sessionNotes
                }) : null
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to complete appointment');
                }
                return response.json();
            })
            .then(data => {
                showNotification('Appointment completed successfully', 'success');

                const modal = bootstrap.Modal.getInstance(completeAppointmentModal);
                modal.hide();

                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            })
            .catch(error => {
                console.error('Error completing appointment:', error);
                showNotification('Failed to complete appointment. Please try again.', 'error');
            });
        });
    }
}

function initializeCalendarView() {
    const calendarElement = document.getElementById('appointmentCalendar');
    if (!calendarElement) return;

    renderBasicCalendar(calendarElement);
}

function renderBasicCalendar(container) {
    const appointments = [];
    const tableRows = document.querySelectorAll('#appointmentsTable tbody tr');

    if (tableRows) {
        tableRows.forEach(row => {
            const clientName = row.querySelector('td:nth-child(1)').textContent;
            const dateText = row.querySelector('td:nth-child(2)').textContent;
            const timeText = row.querySelector('td:nth-child(3)').textContent;
            const status = row.getAttribute('data-status');
            const id = row.querySelector('button[data-appointment-id]')?.getAttribute('data-appointment-id');

            if (id) {
                appointments.push({
                    id: id,
                    clientName: clientName,
                    date: dateText,
                    time: timeText,
                    status: status
                });
            }
        });
    }

    container.innerHTML = '';

    const today = new Date();
    const weekStart = new Date(today);
    weekStart.setDate(today.getDate() - today.getDay());

    const calendarHeader = document.createElement('div');
    calendarHeader.className = 'calendar-header d-flex justify-content-between align-items-center mb-3';
    calendarHeader.innerHTML = `
        <div>
            <button class="btn btn-sm btn-outline-secondary" id="prevWeek">
                <i class="bi bi-chevron-left"></i>
            </button>
            <button class="btn btn-sm btn-outline-secondary ms-2" id="nextWeek">
                <i class="bi bi-chevron-right"></i>
            </button>
        </div>
        <h5 class="mb-0" id="calendarTitle">Week of ${weekStart.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}</h5>
        <div>
            <button class="btn btn-sm btn-outline-primary" id="todayBtn">Today</button>
        </div>
    `;
    container.appendChild(calendarHeader);

    const calendarGrid = document.createElement('div');
    calendarGrid.className = 'calendar-grid';

    const dayHeaders = document.createElement('div');
    dayHeaders.className = 'row mb-2';

    const daysOfWeek = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    daysOfWeek.forEach(day => {
        const dayCol = document.createElement('div');
        dayCol.className = 'col text-center';
        dayCol.innerHTML = `<strong>${day}</strong>`;
        dayHeaders.appendChild(dayCol);
    });

    calendarGrid.appendChild(dayHeaders);

    const dateRow = document.createElement('div');
    dateRow.className = 'row mb-2';

    for (let i = 0; i < 7; i++) {
        const date = new Date(weekStart);
        date.setDate(weekStart.getDate() + i);

        const dateCol = document.createElement('div');
        dateCol.className = 'col text-center';
        dateCol.innerHTML = `<div class="calendar-date">${date.getDate()}</div>`;

        if (date.toDateString() === today.toDateString()) {
            dateCol.querySelector('.calendar-date').className = 'calendar-date bg-primary text-white rounded-circle d-inline-block p-2';
        }

        dateRow.appendChild(dateCol);
    }

    calendarGrid.appendChild(dateRow);

    const appointmentsRow = document.createElement('div');
    appointmentsRow.className = 'row';

    for (let i = 0; i < 7; i++) {
        const date = new Date(weekStart);
        date.setDate(weekStart.getDate() + i);
        const dateString = date.toISOString().split('T')[0];

        const dayCol = document.createElement('div');
        dayCol.className = 'col';
        dayCol.innerHTML = '<div class="calendar-appointments"></div>';

        const appointmentsContainer = dayCol.querySelector('.calendar-appointments');

        const dayAppointments = appointments.filter(apt => {
            const aptDate = new Date(apt.date);
            return aptDate.toISOString().split('T')[0] === dateString;
        });

        if (dayAppointments.length > 0) {
            dayAppointments.forEach(apt => {
                const appointmentDiv = document.createElement('div');
                appointmentDiv.className = 'calendar-appointment mb-2 p-2 rounded';

                if (apt.status === 'SCHEDULED') {
                    appointmentDiv.classList.add('bg-primary', 'text-white');
                } else if (apt.status === 'COMPLETED') {
                    appointmentDiv.classList.add('bg-success', 'text-white');
                } else if (apt.status === 'CANCELLED') {
                    appointmentDiv.classList.add('bg-danger', 'text-white');
                } else {
                    appointmentDiv.classList.add('bg-warning', 'text-white');
                }

                appointmentDiv.innerHTML = `
                    <div><strong>${apt.time}</strong></div>
                    <div>${apt.clientName}</div>
                `;

                appointmentDiv.style.cursor = 'pointer';
                appointmentDiv.addEventListener('click', function() {
                    window.location.href = `/psychologist/appointment/${apt.id}`;
                });

                appointmentsContainer.appendChild(appointmentDiv);
            });
        } else {
            appointmentsContainer.innerHTML = `
                <div class="text-center text-muted py-3">
                    <small>No appointments</small>
                </div>
            `;
        }

        appointmentsRow.appendChild(dayCol);
    }

    calendarGrid.appendChild(appointmentsRow);
    container.appendChild(calendarGrid);

    const prevWeekBtn = container.querySelector('#prevWeek');
    const nextWeekBtn = container.querySelector('#nextWeek');
    const todayBtn = container.querySelector('#todayBtn');

    if (prevWeekBtn) {
        prevWeekBtn.addEventListener('click', function() {
            weekStart.setDate(weekStart.getDate() - 7);
            refreshCalendarView();
        });
    }

    if (nextWeekBtn) {
        nextWeekBtn.addEventListener('click', function() {
            weekStart.setDate(weekStart.getDate() + 7);
            refreshCalendarView();
        });
    }

    if (todayBtn) {
        todayBtn.addEventListener('click', function() {
            weekStart = new Date(today);
            weekStart.setDate(today.getDate() - today.getDay());
            refreshCalendarView();
        });
    }
}

function refreshCalendarView() {
    const calendarElement = document.getElementById('appointmentCalendar');
    if (calendarElement) {
        renderBasicCalendar(calendarElement);
    }
}

function fetchAvailableTimes(clientId) {
    const date = document.getElementById('rescheduleAppointmentDate').value;
    const availableTimesContainer = document.getElementById('rescheduleAvailableTimesContainer');
    
    if (!date) {
        availableTimesContainer.innerHTML = '<div class="alert alert-warning">Please select a date.</div>';
        return;
    }

    availableTimesContainer.innerHTML = `
        <div class="alert alert-info">
            <div class="d-flex align-items-center">
                <div class="spinner-border spinner-border-sm me-2" role="status">
                    <span class="visually-hidden">Loading...</span>
                </div>
                <div>Loading available times...</div>
            </div>
        </div>
    `;

    let psychologistId = document.querySelector('meta[name="psychologistId"]')?.content;

    if (!psychologistId) {
        psychologistId = document.querySelector('input[name="psychologistId"]')?.value;
    }

    if (!psychologistId) {
        const elements = document.querySelectorAll('[data-psychologist-id]');
        if (elements.length > 0) {
            psychologistId = elements[0].getAttribute('data-psychologist-id');
        }
    }

    if (!psychologistId) {
        const appointmentId = document.getElementById('rescheduleAppointmentId').value;
        fetch(`/api/appointments/${appointmentId}`)
            .then(response => response.json())
            .then(appointment => {
                fetchTimesWithPsychologistId(appointment.psychologistId, date);
            })
            .catch(error => {
                console.error("Error fetching appointment details:", error);
                availableTimesContainer.innerHTML = `
                    <div class="alert alert-danger">
                        Error: Could not determine psychologist ID. Please try again later.
                    </div>
                `;
            });
        return;
    }
    
    fetchTimesWithPsychologistId(psychologistId, date);
}

function fetchTimesWithPsychologistId(psychologistId, date) {
    const availableTimesContainer = document.getElementById('rescheduleAvailableTimesContainer');
    
    fetch(`/api/appointments/available?date=${date}&psychologistId=${psychologistId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error fetching available times');
            }
            return response.json();
        })
        .then(availableSlots => {
            updateAvailableTimes(availableSlots);
        })
        .catch(error => {
            console.error('Error fetching available times:', error);
            availableTimesContainer.innerHTML = `
                <div class="alert alert-warning">
                    No available time slots for this date. Please try another date.
                </div>
            `;
        });
}

function updateAvailableTimes(availableSlots) {
    const availableTimesContainer = document.getElementById('rescheduleAvailableTimesContainer');
    
    if (!availableTimesContainer) return;

    if (availableSlots.length === 0) {
        availableTimesContainer.innerHTML = `
            <div class="alert alert-info">
                No available times on this date. Please select another date.
            </div>
        `;
        return;
    }

    availableTimesContainer.innerHTML = '';

    const timeGrid = document.createElement('div');
    timeGrid.classList.add('row', 'g-2');

    availableSlots.forEach(slot => {
        const startTime = new Date(slot.startTime);
        const endTime = new Date(slot.endTime);
        const formattedStartTime = startTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const formattedEndTime = endTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

        const timeCol = document.createElement('div');
        timeCol.classList.add('col-md-3', 'col-6');

        const timeBtn = document.createElement('button');
        timeBtn.type = 'button';
        timeBtn.classList.add('btn', 'btn-outline-primary', 'w-100', 'mb-2');
        timeBtn.innerText = `${formattedStartTime} - ${formattedEndTime}`;
        timeBtn.dataset.startTime = slot.startTime;
        timeBtn.dataset.endTime = slot.endTime;

        timeBtn.addEventListener('click', function() {
            availableTimesContainer.querySelectorAll('.btn-primary').forEach(btn => {
                btn.classList.remove('btn-primary');
                btn.classList.add('btn-outline-primary');
            });

            this.classList.remove('btn-outline-primary');
            this.classList.add('btn-primary');

            document.getElementById('rescheduleStartTime').value = this.dataset.startTime;
            document.getElementById('rescheduleEndTime').value = this.dataset.endTime;
        });

        timeCol.appendChild(timeBtn);
        timeGrid.appendChild(timeCol);
    });

    availableTimesContainer.appendChild(timeGrid);
}

function showNotification(message, type = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.setAttribute('role', 'alert');
    alertDiv.innerHTML = `
        ${message}
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