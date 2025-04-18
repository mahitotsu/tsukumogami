<script setup lang="ts">
import { ref } from 'vue';

const prompt = ref('');
const answer = ref('');

const send = async () => {
    const data = await $fetch('/api/my-agent', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: { prompt: prompt.value }
    });
    answer.value = data;
}
</script>
<template>
    <h1>TSUKUMOGAMI</h1>
    <input type="text" v-model="prompt" placeholder="Enter new prompt" />
    <button @click="send">send</button>
    <div>{{ answer }}</div>
</template>