import { sha256 } from 'js-sha256';

export const useApiClient = () => {

    const post = async (url: string, payload: string): Promise<any> => {
        const data = await $fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'x-amz-content-sha256': sha256(payload),
            },
            body: payload,
        });
        return data;
    };

    return { post };
} 