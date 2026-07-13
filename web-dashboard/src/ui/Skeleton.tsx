// Placeholder de carregamento: evita "pulo" de layout e o texto cru "carregando".
export function Skeleton({ lines = 3 }: { lines?: number }) {
  return (
    <div className="skeleton-group" aria-hidden="true">
      {Array.from({ length: lines }, (_, index) => (
        <div key={index} className="skeleton" style={{ width: `${100 - (index % 3) * 14}%` }} />
      ))}
    </div>
  );
}
