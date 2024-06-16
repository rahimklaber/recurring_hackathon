async function signmandate(txblob, network){
    let result = await window.freighterApi.signTransaction(txblob, network)

    console.log(result)

    const urlParams = new URLSearchParams(window.location.search);

    // window.location.href = `/signmandate?id=${urlParams.get("id")}&address=${urlParams.get("address")}`;
    await fetch(`http://localhost:8080/signmandate?id=${urlParams.get("id")}&address=${urlParams.get("address")}&tx=${encodeURIComponent(result)}`, {
        method: "POST",
    }).then(response => {
        if (response.redirected) {
            window.location.href = response.url;
        }
    })
        .catch(function(err) {
            console.info(err + " url: " + url);
        });
}