$( document ).ready(function() {
    $.get({
        url: "/slots",
        success: function (slots) {
            fillDates(slots);
            fillTimes(slots);
        }
    });
    fillNbOfGuest();
});

function fillDates(slots) {
    let searchParams = new URLSearchParams(window.location.search)
    let lang = searchParams.get('lang')
    if (lang === null) {
        lang = "en";
    }
    let dateInput = $('#dateInput');
    const now = new Date();
    const today = now.toISOString().substring(0, 10);
    now.setDate(now.getDate() + 1);
    const tomorrow = now.toISOString().substring(0, 10);
    for (let i = 0; i < slots.length; i++) {
        let value
        if (slots[i].date === today) {
            value = $("#today").text();
        } else if (slots[i].date === tomorrow) {
            value = $("#tomorrow").text();
        } else {
            const date = new Date(slots[i].date);
            value = date.toLocaleString(lang, { month: 'long', weekday: 'long', day: 'numeric' });
        }
        dateInput.append($("<option></option>").attr("value", slots[i].date).text(value));
    }

    dateInput.change(function () { fillTimes(slots); })
}

function fillTimes(slots) {
    const timeInput = $('#timeInput');
    timeInput.empty();
    const selectedSlot = findSelectedDate(slots);
    for (let i = 0; i < selectedSlot.slotTimes.length; i++) {
        const time = selectedSlot.slotTimes[i].time;
        const enabled = selectedSlot.slotTimes[i].free;
        timeInput.append($("<option></option>").attr("value", time).attr("disabled", !enabled).text(time));
    }
    const isOptionEmpty = selectedSlot.slotTimes.length === 0
    if (isOptionEmpty) {
        timeInput.append($("<option></option>").text($("#noavailabletimetoday").text()));
    }
    const elementsToDisableIfNoSlots = ["#timeInput", "#submitButton", "#nbOfGuest"];
    elementsToDisableIfNoSlots.forEach(function(item) {
        $(item).prop('disabled', isOptionEmpty);
    });
}

function findSelectedDate(slots) {
    const selectedDate = $("#dateInput").find("option:selected").val();
    for (let i = 0; i < slots.length; i++) {
        if (slots[i].date === selectedDate) {
            return slots[i];
        }
    }

}

function fillNbOfGuest() {
    let nbOfGuests = $('#nbOfGuests');
    const oneHouseholdOnlyInfo = $("#oneHouseholdOnlyInfo").text();
    const maxGuestInForm = parseInt($("#maxGuestInForm").text());
    const oneHouseHoldLimitInForm = parseInt($("#oneHouseHoldLimitInForm").text());
    for (let i = 1; i <= maxGuestInForm; i++) {
        let text = i;
        if (i > oneHouseHoldLimitInForm) {
           text += " " + oneHouseholdOnlyInfo;
        }
        nbOfGuests.append($("<option></option>").attr("value", i).text(text));
    }
    nbOfGuests.change(function () {
        const guests = parseInt(nbOfGuests.find("option:selected").val());
        if (guests > oneHouseHoldLimitInForm) {
            $('#familyCheckGroup').show();
            $('#familyCheck').prop('required', true);
        } else {
            $('#familyCheckGroup').hide();
            $('#familyCheck').prop('required', false);
        }
    })
}

function onSubmit() {
    let form = $("#reservationForm");
    form.addClass('was-validated');
    if (form[0].checkValidity()) {
        $('#submitButton').attr("disabled", true)
        form.submit();
    }
}