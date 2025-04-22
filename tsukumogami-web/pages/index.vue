<script setup lang="ts">
import { ref } from 'vue';
import { useApiClient } from '~/components/useApiClient';

const loading = ref(false);
const prompt = ref('');
const answer = ref('');
const trace = ref('');

const send = async () => {
    loading.value = true;
    try {
        const res = await useApiClient().post('/api/my-agent', JSON.stringify({ prompt: prompt.value }));
        answer.value = res.answer;
        trace.value = res.trace;
    } finally {
        loading.value = false;
    }
}
</script>

<template>
    <h1>TSUKUMOGAMI</h1>
    <h4>prompt</h4>
    <textarea v-model="prompt" placeholder="Enter new prompt" rows="6" style="width: 100%; resize: vertical;" />
    <button @click="send">send</button>
    <h4>answer</h4>
    <p style="white-space: pre-line;">{{ answer }}</p>
    <h4>trace</h4>
    <p style="white-space: pre-line;">{{ trace }}</p>

    <div v-if="loading"
        style="position: fixed; z-index: 9999; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(255,255,255,0.7); display: flex; align-items: center; justify-content: center;">
        <div class="spinner"></div>
    </div>
</template>

<style scoped>
.spinner {
    border: 8px solid #f3f3f3;
    border-top: 8px solid #3498db;
    border-radius: 50%;
    width: 60px;
    height: 60px;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% {
        transform: rotate(0deg);
    }

    100% {
        transform: rotate(360deg);
    }
}
</style>