<script setup lang="ts">
import { ref } from 'vue';
import { useApiClient } from '~/components/useApiClient';

const prompt = ref('');
const answer = ref('');
const trace = ref('');

const send = async () => {
    const res = await useApiClient().post('/api/my-agent', JSON.stringify({ prompt: prompt.value }));
    answer.value = res.answer;
    trace.value = res.trace;
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
</template>