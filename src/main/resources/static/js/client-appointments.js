console.log('Client appointments script loaded');

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM Content Loaded');

    setupAppointmentCancellation();

    setupAppointmentFilters();

    setupViewFilters();

    initializeAppointmentForm();

    console.log('All event listeners set up');
});

function setupAppointmentCancellation() {
    console.log('Setting up appointment cancellation');

    const cancelModal = document.getElementById('cancelAppointmentModal');
    if (cancelModal) {
        cancelModal.addEventListener('show.bs.modal', function(event) {
            console.log('Cancel modal opened');
            const button = event.relatedTarget;
            const appointmentId = button.getAttribute('data-appointment-id');
            const psychologistId = button.getAttribute('data-psychologist-id');
            
            console.log('Appointment ID captured:', appointmentId);
            console.log('Psychologist ID captured:', psychologistId);

            document.getElementById('cancelAppointmentId').value = appointmentId;
            document.getElementById('cancelPsychologistId').value = psychologistId;
        });
    }

    const confirmCancelBtn = document.getElementById('confirmCancelButton');
    if (confirmCancelBtn) {
        console.log('Confirm cancel button found');
        confirmCancelBtn.addEventListener('click', function() {
            console.log('Confirm cancel button clicked');
            
            const appointmentId = document.getElementById('cancelAppointmentId').value;
            const psychologistId = document.getElementById('cancelPsychologistId').value;
            
            console.log('Using appointment ID:', appointmentId);
            console.log('Using psychologist ID:', psychologistId);
            
            if (!appointmentId || !psychologistId) {
                console.error('Missing appointment ID or psychologist ID');
                alert('Error: Missing appointment information. Please try again.');
                return;
            }

            this.disabled = true;
            const originalText = this.innerHTML;
            this.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Cancelling...';

            const url = `/api/appointments/${appointmentId}/status?status=CANCELLED&psychologistId=${psychologistId}`;
            console.log('Sending request to:', url);
            
            fetch(url, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    notes: 'Cancelled by client'
                })
            })
            .then(response => {
                console.log('Response status:', response.status);
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(`Failed to cancel appointment: ${text || 'Unknown error'}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('Success response:', data);
                alert('Appointment cancelled successfully!');
                window.location.reload();
            })
            .catch(error => {
                console.error('Error cancelling appointment:', error);
                alert(error.message || 'Failed to cancel appointment. Please try again.');

                this.disabled = false;
                this.innerHTML = originalText;
            });
        });
    } else {
        console.warn('Confirm cancel button not found');
    }
}

function setupAppointmentFilters() {
    console.log('Setting up appointment filters');
    
    const filterStatus = document.getElementById('filterStatus');
    const filterPsychologist = document.getElementById('filterPsychologist');
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
            if (filterPsychologist) filterPsychologist.value = '';
            if (filterDateFrom) filterDateFrom.value = '';
            if (filterDateTo) filterDateTo.value = '';
            filterAppointments();
        });
    }

    function filterAppointments() {
        if (!tableRows) return;

        const status = filterStatus ? filterStatus.value : '';
        const psychologist = filterPsychologist ? filterPsychologist.value : '';
        const dateFrom = filterDateFrom ? filterDateFrom.value : '';
        const dateTo = filterDateTo ? filterDateTo.value : '';

        tableRows.forEach(row => {
            let showRow = true;

            if (status && row.getAttribute('data-status') !== status) {
                showRow = false;
            }

            if (showRow && psychologist && row.getAttribute('data-psychologist') !== psychologist) {
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

function setupViewFilters() {
    console.log('Setting up view filters');
    
    const viewButtons = document.querySelectorAll('[data-view]');
    const tableRows = document.querySelectorAll('#appointmentsTable tbody tr');
    
    if (viewButtons.length > 0) {
        viewButtons.forEach(button => {
            button.addEventListener('click', function() {
                viewButtons.forEach(btn => btn.classList.remove('active'));

                this.classList.add('active');

                const view = this.getAttribute('data-view');
                filterByView(view);
            });
        });
    }

    function filterByView(view) {
        if (!tableRows) return;

        const now = new Date().toISOString().slice(0, 10);

        tableRows.forEach(row => {
            const rowDate = row.getAttribute('data-date');
            const rowStatus = row.getAttribute('data-status');

            let showRow = true;

            if (view === 'upcoming') {
                showRow = rowDate >= now && rowStatus === 'SCHEDULED';
            } else if (view === 'past') {
                showRow = rowDate < now || rowStatus === 'COMPLETED' || rowStatus === 'CANCELLED' || rowStatus === 'NO_SHOW';
            }

            row.style.display = showRow ? '' : 'none';
        });
    }

    if (tableRows && tableRows.length > 0) {
        filterByView('all');
    }
}

function initializeAppointmentForm() {
    console.log('Initializing appointment form');
    const appointmentForm = document.getElementById('appointmentForm');
    
    if (appointmentForm) {
        console.log('Appointment form found');
        const psychologistSelect = document.getElementById('psychologistId');
        const dateInput = document.getElementById('appointmentDate');
        const startTimeInput = document.getElementById('startTime');
        const endTimeInput = document.getElementById('endTime');
        const availableTimesContainer = document.getElementById('availableTimesContainer');
        const submitButton = appointmentForm.querySelector('button[type="submit"]');

        if (!psychologistSelect || !dateInput || !startTimeInput || !endTimeInput || !availableTimesContainer || !submitButton) {
            console.log('Required elements not found:',
                        {psychologistSelect: !!psychologistSelect,
                         dateInput: !!dateInput,
                         startTimeInput: !!startTimeInput,
                         endTimeInput: !!endTimeInput,
                         availableTimesContainer: !!availableTimesContainer,
                         submitButton: !!submitButton});
            return;
        }

        const today = new Date().toISOString().split('T')[0];
        dateInput.min = today;

        dateInput.value = today;

        psychologistSelect.addEventListener('change', function() {
            if (this.value && dateInput.value) {
                fetchAvailableTimes();
            }
        });

        dateInput.addEventListener('change', function() {
            if (this.value && psychologistSelect.value) {
                fetchAvailableTimes();
            }
        });

        appointmentForm.addEventListener('submit', function(e) {
            console.log('Appointment form submitted');
            e.preventDefault();

            if (!startTimeInput.value || !endTimeInput.value) {
                console.log('No time selected');
                alert('Please select an appointment time.');
                return false;
            }

            submitButton.disabled = true;
            const originalButtonText = submitButton.innerHTML;
            submitButton.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Scheduling...';

            const formData = new FormData(this);
            const data = new URLSearchParams();
            for (const pair of formData) {
                data.append(pair[0], pair[1]);
            }

            fetch(this.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: data.toString()
            })
            .then(response => {
                if (response.ok) {
                    window.location.reload();
                } else {
                    return response.text().then(text => {
                        throw new Error(`Failed to schedule appointment: ${text || 'Unknown error'}`);
                    });
                }
            })
            .catch(error => {
                console.error('Error scheduling appointment:', error);
                alert('Error scheduling appointment: ' + error.message);

                submitButton.disabled = false;
                submitButton.innerHTML = originalButtonText;
            });
        });

        function fetchAvailableTimes() {
            const psychologistId = psychologistSelect.value;
            const date = dateInput.value;

            if (!psychologistId || !date) return;

            availableTimesContainer.innerHTML = `
                <div class="d-flex justify-content-center">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            `;

            fetch(`/api/appointments/available?psychologistId=${psychologistId}&date=${date}`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Error fetching available times');
                    }
                    return response.json();
                })
                .then(timeSlots => {
                    updateAvailableTimes(timeSlots);
                })
                .catch(error => {
                    console.error('Error fetching available times:', error);
                    availableTimesContainer.innerHTML = `
                        <div class="alert alert-danger">
                            Failed to load available times. Please try again.
                        </div>
                    `;
                });
        }

        function updateAvailableTimes(availableSlots) {
            if (!availableTimesContainer) return;

            availableTimesContainer.innerHTML = '';

            if (availableSlots.length === 0) {
                availableTimesContainer.innerHTML = `
                    <div class="alert alert-info">
                        No available times on this date. Please select another date.
                    </div>
                `;
                return;
            }

            const timeGrid = document.createElement('div');
            timeGrid.classList.add('row', 'g-2');

            availableSlots.forEach(slot => {
                if (!slot.available) return;

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
                    document.querySelectorAll('#availableTimesContainer .btn-primary').forEach(btn => {
                        btn.classList.remove('btn-primary');
                        btn.classList.add('btn-outline-primary');
                    });

                    this.classList.remove('btn-outline-primary');
                    this.classList.add('btn-primary');

                    startTimeInput.value = this.dataset.startTime;
                    endTimeInput.value = this.dataset.endTime;
                });

                timeCol.appendChild(timeBtn);
                timeGrid.appendChild(timeCol);
            });

            availableTimesContainer.appendChild(timeGrid);
        }

        if (psychologistSelect.value) {
            fetchAvailableTimes();
        }
    } else {
        console.log('Appointment form not found');
    }
}