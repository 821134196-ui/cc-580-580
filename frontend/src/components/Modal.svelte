<script>
  export let title = ''
  export let onClose = () => {}
</script>

<div class="overlay" on:click={onClose} role="presentation">
  <div class="modal" on:click|stopPropagation role="dialog" aria-label={title}>
    <div class="modal-head">
      <h3>{title}</h3>
      <button class="x" on:click={onClose}>✕</button>
    </div>
    <div class="modal-body">
      <slot />
    </div>
  </div>
</div>

<style>
  .overlay {
    position: fixed; inset: 0; background: rgba(15, 23, 42, 0.45);
    display: flex; align-items: center; justify-content: center; z-index: 50;
  }
  .modal {
    background: #fff; border-radius: 14px; width: min(560px, 92vw);
    max-height: 86vh; overflow: auto; box-shadow: 0 20px 50px rgba(15, 23, 42, 0.35);
  }
  .modal-head {
    display: flex; justify-content: space-between; align-items: center;
    padding: 14px 20px; border-bottom: 1px solid #e5e7eb;
  }
  .modal-head h3 { margin: 0; font-size: 15px; }
  .x { border: none; background: none; font-size: 15px; cursor: pointer; color: #6b7280; }
  .modal-body { padding: 18px 20px; }
</style>
