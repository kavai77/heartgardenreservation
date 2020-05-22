$( document ).ready(function() {
    let searchParams = new URLSearchParams(window.location.search)
    let dateStr = searchParams.get('date')
    if (dateStr === null) {
        dateStr = new Date().toISOString().substring(0, 10);
    }
    $("#date").text(dateStr)
    let date = new Date(dateStr)
    date.setDate(date.getDate() - 1);
    $("#prevLink").prop("href", "?date="+date.toISOString().substring(0, 10));
    date.setDate(date.getDate() + 2);
    $("#nextLink").prop("href", "?date="+date.toISOString().substring(0, 10));
    $.get({
        url: "/reservations",
        data: {
            fromDate: dateStr,
            toDate: dateStr
        },
        success: function (data) {
            let tbody = $("#tbody");
            for (let i = 0; i < data.length; i++) {
                tbody.append($('<tr>')
                        .append($('<td>').text(data[i].date))
                        .append($('<td>').text(data[i].times.join(", ")))
                        .append($('<td>').text(data[i].name))
                        .append($('<td>').text(data[i].email))
                        .append($('<td>').text(data[i].nbOfGuests))
                        .append($('<td>').text(new Date(data[i].registered).toLocaleString('en-us')))
                    );
            }
        }
    });
});