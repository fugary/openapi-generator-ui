const { createApp, ref } = Vue

createApp({
    setup() {
        const apiParam = ref(initialApiParam)
        return {
            apiParam
        }
    }
}).mount('#app')