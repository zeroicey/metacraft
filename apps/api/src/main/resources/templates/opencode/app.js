const { createApp, ref } = Vue;

createApp({
  setup() {
    const title = ref('MetaCraft App');
    const subtitle = ref('应用骨架已准备完成，现在可以继续按需求生成和修改。');

    return {
      title,
      subtitle,
    };
  },
  template: `
    <main class="min-h-screen px-6 py-12">
      <section class="mx-auto flex min-h-[70vh] max-w-4xl items-center justify-center">
        <div class="w-full rounded-[32px] bg-white p-10 shadow-2xl shadow-slate-200/60 ring-1 ring-slate-200">
          <div class="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-sm text-slate-600">
            <i class="bi bi-stars"></i>
            <span>MetaCraft Template</span>
          </div>
          <h1 class="mt-6 text-4xl font-semibold tracking-tight text-slate-900">{{ title }}</h1>
          <p class="mt-4 max-w-2xl text-lg leading-8 text-slate-600">{{ subtitle }}</p>
        </div>
      </section>
    </main>
  `,
}).mount('#app');