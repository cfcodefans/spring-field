function info(str: string): void {
    console.info(str)
    const ol: HTMLOListElement = document.getElementById("output") as HTMLOListElement
    const li: HTMLLIElement = document.createElement("li")
    li.innerText = (new Date()).toISOString() + "    " + str
    ol.insertBefore(li, ol.firstChild)
}

async function main() {
    info("Hello, world!")
    //    await fetch("/api/now").then((response) => {
    //         if (response.ok) {
    //             return response.json()
    //         } else {
    //             throw new Error(`Network response was not ok ${response.status}`)
    //         }
    //     }).then((data) => {
    //         info("Current time: " + data.now)
    //     }).catch((error) => {
    //         info("Fetch error: " + error.message)
    //     })

    const params = new URLSearchParams()
    params.append('username', "joe")
    params.append('pwd', "secret")

    await fetch("/auth/login", {
        method: "POST",
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: params.toString()
    }).then((response) => {
        if (response.ok) {
            return response.text()
        } else {
            throw new Error(`Network response was not ok ${response.status}`)
        }
    }).then((data) => {
        info("Login response: " + data)
    }).catch((error) => {
        info("Fetch error: " + error.message)
    })

    await fetch("/api/now").then((response) => {
        if (response.ok) {
            return response.text()
        } else {
            throw new Error(`Network response was not ok ${response.status}`)
        }
    }).then((data) => {
        info("Current time: " + data)
    }).catch((error) => {
        info("Fetch error: " + error.message)
    })
}
main()
