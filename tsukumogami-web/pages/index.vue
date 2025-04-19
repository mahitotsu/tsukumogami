<script setup lang="ts">
import { ref } from 'vue';
import { useApiClient } from '~/components/useApiClient';

const prompt = ref('');
const answer = ref('');

const send = async () => {
    answer.value = await useApiClient().post('/api/my-agent', JSON.stringify({ prompt: prompt.value }));
}
</script>
<template>
    <h1>TSUKUMOGAMI</h1>
    <input type="text" v-model="prompt" placeholder="Enter new prompt" />
    <button @click="send">send</button>
    <div>{{ answer }}</div>
</template>