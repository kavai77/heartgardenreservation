$( document ).ready(function() {
    $.get({
        url: "/slots",
        success: function (data) {
            slots = data;
            fillDates();
            fillTimes();
        }
    });
    fillNbOfGuest();
    setupFormUpdate();
});

var elementsToDisableIfNoSlots = ["#timeInput", "#submit", "#nbOfGuest"]
var slots;

function fillDates() {
    let dateInput = $('#dateInput');
    const now = new Date();
    const today = now.toISOString().substring(0, 10);
    now.setDate(now.getDate() + 1);
    const tomorrow = now.toISOString().substring(0, 10);
    for (i = 0; i < slots.length; i++) {
        let value
        if (slots[i].date === today) {
            value = "Today";
        } else if (slots[i].date === tomorrow) {
            value = "Tomorrow"
        } else {
            const date = new Date(slots[i].date);
            value = date.toLocaleString('en-us', { month: 'long', weekday: 'long', day: 'numeric' });
        }
        dateInput.append($("<option></option>").attr("value", slots[i].date).text(value));
    }

    dateInput.on('change', fillTimes)
}

function fillTimes() {
    const timeInput = $('#timeInput');
    timeInput.empty();
    const selectedSlot = findSelectedDate();
    for (i = 0; i < selectedSlot.slotTimes.length; i++) {
        const time = selectedSlot.slotTimes[i].time;
        const enabled = selectedSlot.slotTimes[i].free;
        timeInput.append($("<option></option>").attr("value", time).attr("disabled", !enabled).text(time));
    }
    const isOptionEmpty = selectedSlot.slotTimes.length === 0
    if (isOptionEmpty) {
        timeInput.append($("<option></option>").text("No available times today"));
    }
    elementsToDisableIfNoSlots.forEach(function(item) {
        $(item).prop('disabled', isOptionEmpty);
    });
}

function findSelectedDate() {
    const selectedDate = $("#dateInput").find("option:selected").val();
    for (i = 0; i < slots.length; i++) {
        if (slots[i].date === selectedDate) {
            return slots[i];
        }
    }

}

function fillNbOfGuest() {
    for (i = 1; i <= 4; i++) {
        $('#nbOfGuests').append($("<option></option>").attr("value", i).text(i));
    }
}

function setupFormUpdate() {
    $("#reservationForm").submit(function(e) {
        $(this).addClass('was-validated');
        return $(this)[0].checkValidity()
    });
}