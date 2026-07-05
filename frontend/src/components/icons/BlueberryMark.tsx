import vlvLogo from '../../assets/vlv-logo.png';

interface BlueberryMarkProps {
  compact?: boolean;
}

export function BlueberryMark({ compact = false }: BlueberryMarkProps) {
  return (
    <span className={compact ? 'brand__mark brand__mark--logo brand__mark--compact' : 'brand__mark brand__mark--logo'} aria-hidden="true">
      <img src={vlvLogo} alt="" loading="eager" />
    </span>
  );
}
