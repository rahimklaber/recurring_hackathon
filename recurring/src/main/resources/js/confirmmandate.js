async function confirmMandate() {
    let address = await window.freighterApi.getPublicKey()
    const urlParams = new URLSearchParams(window.location.search);

    window.location.href = `/signmandate?id=${urlParams.get("id")}&address=${address}`;
}