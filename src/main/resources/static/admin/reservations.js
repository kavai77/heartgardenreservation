let date = new Date()

$( document ).ready(function() {
    $("#prevLink").click(function () {
        date.setDate(date.getDate() - 1);
        changeDate();
    });
    $("#nextLink").click(function () {
        date.setDate(date.getDate() + 1);
        changeDate();
    });
    changeDate();
});

function changeDate() {
    $("#mainContent").hide();
    $("#spinner").show();
    $.get({
        url: "/admin/reservations",
        data: {
            fromDate: getIsoDate(),
            toDate: getIsoDate()
        },
        success: function (data) {
            $("#spinner").hide();
            $("#mainContent").show();
            $("#date").text(getIsoDate());
            let tbody = $("#tbody");
            tbody.empty();
            for (let i = 0; i < data.length; i++) {
                tbody.append($('<tr>')
                    .append($('<td>').text(data[i].date))
                    .append($('<td>').text(data[i].times.join(", ")))
                    .append($('<td>').text(data[i].name))
                    .append($('<td>').text(data[i].email))
                    .append($('<td>').text(data[i].nbOfGuests))
                    .append($('<td>').text(new Date(data[i].registered).toLocaleString('en-us')))
                    .append($('<td>')
                        .append($('<button>')
                            .addClass("btn")
                            .addClass("btn-danger")
                            .addClass("deleteButton")
                            .prop('type', 'button')
                            .data('customerUUID', data[i].customerUUID)
                            .data('customerName', data[i].name)
                            .text('Delete')
                        )
                    )
                );
            }
            $(".deleteButton").click(function () {
                let thisButton = $(this);
                if (window.confirm("Are you sure you want to delete the registration for " + thisButton.data("customerName") + "?")) {
                    $.ajax({
                        url: "/admin/reservation",
                        method: 'DELETE',
                        data: {
                            customerUUID: thisButton.data("customerUUID")
                        },
                        success: function () {
                            location.reload();
                        }
                    })
                }
            })
        }
    });
}

function getIsoDate() {
    return date.toISOString().substring(0, 10);
}